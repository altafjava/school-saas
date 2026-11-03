package com.altafjava.school.application.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import com.altafjava.school.domain.importing.model.ImportType;
import com.altafjava.school.domain.importing.repository.ImportJobRepository;

class ImportRunnerTest {

	private ImportRunner runner;
	private RecordingImporter importer;

	@BeforeEach
	void setUp() {
		PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
		when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
		runner = new ImportRunner(transactions, mock(ImportJobRepository.class));
		importer = new RecordingImporter();
	}

	@Test
	void dryRun_reportsBadRowsAndRepeatsWithinTheFile() {
		List<RowFailure> failures = runner.dryRun(importer,
				List.of(row(2, "a"), row(3, "bad"), row(4, "a"), row(5, "b")));

		assertEquals(List.of(3, 4), failures.stream().map(RowFailure::lineNumber).toList());
		assertEquals("code", failures.getFirst().field());
		assertTrue(failures.get(1).message().contains("Repeats line 2"));
	}

	@Test
	void run_rollsBackTheWholeBatchOfABadRow_andKeepsTheOtherBatches() {
		List<ImportRow> rows = new ArrayList<>(IntStream.range(0, 150).mapToObj(i -> row(i + 2, "ok" + i)).toList());
		rows.set(120, row(122, "bad"));

		List<RowFailure> failures = runner.run(importer, rows, Map.of());

		assertEquals(50, failures.size());
		assertTrue(failures.stream().allMatch(failure -> failure.lineNumber() >= 102));
		assertTrue(
				failures.stream().anyMatch(failure -> failure.lineNumber() == 122 && "code".equals(failure.field())));
		assertTrue(failures.stream().anyMatch(failure -> failure.message().contains("line 122 in the same batch")));
	}

	@Test
	void run_skipsWhatTheDryRunRejected_andReportsItAgain() {
		RowFailure rejected = new RowFailure(3, "code", "rejected earlier");

		List<RowFailure> failures = runner.run(importer, List.of(row(2, "a"), row(3, "b")), Map.of(3, rejected));

		assertEquals(List.of(rejected), failures);
		assertEquals(List.of("a"), importer.applied);
	}

	private static ImportRow row(int line, String code) {
		return new ImportRow(line, Map.of("code", code));
	}

	private static final class RecordingImporter implements RowImporter {

		private final List<String> applied = new ArrayList<>();

		@Override
		public ImportType type() {
			return ImportType.STUDENTS;
		}

		@Override
		public String requiredPermission() {
			return "ANY";
		}

		@Override
		public List<ImportField> fields() {
			return List.of(ImportField.required("code", "Code"));
		}

		@Override
		public Optional<String> uniqueKey(ImportRow row) {
			return Optional.of(row.requiredText("code"));
		}

		@Override
		public void apply(ImportRow row) {
			if ("bad".equals(row.text("code"))) {
				throw new ImportRowException("code", "bad code");
			}
			applied.add(row.text("code"));
		}
	}
}
