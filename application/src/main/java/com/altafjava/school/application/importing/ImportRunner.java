package com.altafjava.school.application.importing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.school.domain.importing.repository.ImportJobRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs an importer over rows. A dry run applies every row in a transaction that is always rolled
 * back, flushing first so database rules are checked too. An import commits in batches: every row of
 * a batch goes in, or none of them does, and the rest of the file still runs.
 */
@Slf4j
@Component
class ImportRunner {

	static final int BATCH_SIZE = 100;

	private final TransactionTemplate isolated;
	private final ImportJobRepository flusher;

	ImportRunner(PlatformTransactionManager transactionManager, ImportJobRepository importJobRepository) {
		this.isolated = new TransactionTemplate(transactionManager);
		this.isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.flusher = importJobRepository;
	}

	/** Every failure the import would meet; duplicates within the file are caught before any write. */
	List<RowFailure> dryRun(RowImporter importer, List<ImportRow> rows) {
		List<RowFailure> failures = new ArrayList<>();
		Map<String, Integer> firstLineByKey = new HashMap<>();
		for (ImportRow row : rows) {
			RowFailure duplicate = duplicateOf(importer, row, firstLineByKey);
			if (duplicate != null) {
				failures.add(duplicate);
				continue;
			}
			try {
				isolated.executeWithoutResult(status -> {
					importer.apply(row);
					flusher.flush();
					status.setRollbackOnly();
				});
			} catch (RuntimeException e) {
				failures.add(failureOf(row, e));
			}
		}
		return failures;
	}

	/** Imports every row not in {@code skipped}; returns the failures, the skipped lines' own included. */
	List<RowFailure> run(RowImporter importer, List<ImportRow> rows, Map<Integer, RowFailure> skipped) {
		List<RowFailure> failures = new ArrayList<>(skipped.values());
		List<ImportRow> toImport = rows.stream().filter(row -> !skipped.containsKey(row.lineNumber())).toList();
		for (int from = 0; from < toImport.size(); from += BATCH_SIZE) {
			failures.addAll(runBatch(importer, toImport.subList(from, Math.min(from + BATCH_SIZE, toImport.size()))));
		}
		return failures;
	}

	private List<RowFailure> runBatch(RowImporter importer, List<ImportRow> batch) {
		ImportRow[] current = new ImportRow[1];
		try {
			isolated.executeWithoutResult(status -> {
				for (ImportRow row : batch) {
					current[0] = row;
					importer.apply(row);
				}
				current[0] = null;
				flusher.flush();
			});
			return List.of();
		} catch (RuntimeException e) {
			log.warn("action=import-batch-rolled-back firstLine={} rows={} reason={}", batch.getFirst().lineNumber(),
					batch.size(), e.getClass().getSimpleName());
			return batchFailures(batch, current[0], e);
		}
	}

	// The row at fault gets the reason; the others in its batch say why they were not imported either.
	private static List<RowFailure> batchFailures(List<ImportRow> batch, ImportRow culprit, RuntimeException e) {
		List<RowFailure> failures = new ArrayList<>();
		for (ImportRow row : batch) {
			if (culprit != null && row.lineNumber() == culprit.lineNumber()) {
				failures.add(failureOf(row, e));
			} else {
				failures.add(new RowFailure(row.lineNumber(), null, culprit == null
						? "Not imported: the batch could not be saved (" + messageOf(e) + ")"
						: "Not imported: line " + culprit.lineNumber() + " in the same batch failed"));
			}
		}
		return failures;
	}

	private static RowFailure duplicateOf(RowImporter importer, ImportRow row, Map<String, Integer> firstLineByKey) {
		return importer.uniqueKey(row)
				.map(key -> firstLineByKey.putIfAbsent(key, row.lineNumber()))
				.map(first -> new RowFailure(row.lineNumber(), null, "Repeats line " + first + " of this file"))
				.orElse(null);
	}

	private static RowFailure failureOf(ImportRow row, RuntimeException e) {
		String field = e instanceof ImportRowException rowError ? rowError.getField() : null;
		return new RowFailure(row.lineNumber(), field, messageOf(e));
	}

	private static String messageOf(RuntimeException e) {
		Throwable cause = e;
		while (cause.getCause() != null && cause.getMessage() == null) {
			cause = cause.getCause();
		}
		return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
	}

	static Set<Integer> lines(List<RowFailure> failures) {
		return failures.stream().map(RowFailure::lineNumber).collect(Collectors.toSet());
	}
}
