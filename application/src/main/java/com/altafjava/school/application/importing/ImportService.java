package com.altafjava.school.application.importing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.error.SchoolErrorCode;
import com.altafjava.school.domain.importing.model.ImportJob;
import com.altafjava.school.domain.importing.model.ImportJobError;
import com.altafjava.school.domain.importing.model.ImportJobStatus;
import com.altafjava.school.domain.importing.model.ImportPhase;
import com.altafjava.school.domain.importing.model.ImportType;
import com.altafjava.school.domain.importing.repository.ImportJobErrorRepository;
import com.altafjava.school.domain.importing.repository.ImportJobRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Bulk import in steps a school can check before anything is written: upload a CSV, map its columns
 * to the fields, dry-run it for a full error report, then import it once. Each job is kept as
 * history with its last report.
 */
@Service
public class ImportService {

	public static final int MAX_FILE_BYTES = 1_048_576;
	private static final int PREVIEW_ROWS = 10;
	private static final char BYTE_ORDER_MARK = '﻿';
	private static final TypeReference<Map<String, String>> MAPPING = new TypeReference<>() {
	};

	private final Map<ImportType, RowImporter> importers;
	private final ImportJobRepository importJobRepository;
	private final ImportJobErrorRepository importJobErrorRepository;
	private final ImportRunner importRunner;
	private final PermissionAuthorizationService permissionAuthorizationService;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public ImportService(List<RowImporter> importers, ImportJobRepository importJobRepository,
			ImportJobErrorRepository importJobErrorRepository, ImportRunner importRunner,
			PermissionAuthorizationService permissionAuthorizationService, ObjectMapper objectMapper,
			PlatformTransactionManager transactionManager) {
		this.importers = importers.stream().collect(Collectors.toMap(RowImporter::type, Function.identity(),
				(first, other) -> first, () -> new EnumMap<>(ImportType.class)));
		this.importJobRepository = importJobRepository;
		this.importJobErrorRepository = importJobErrorRepository;
		this.importRunner = importRunner;
		this.permissionAuthorizationService = permissionAuthorizationService;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public List<ImportTypeDescription> types() {
		return importers.values().stream().sorted(Comparator.comparing(RowImporter::type))
				.map(importer -> new ImportTypeDescription(importer.type(), importer.fields())).toList();
	}

	/** Reads the file, suggests a mapping from its column names and keeps it as a new job. */
	public ImportJobView upload(ImportType type, String fileName, byte[] bytes, Long uploadedByUserId) {
		RowImporter importer = importerFor(type);
		if (bytes.length > MAX_FILE_BYTES) {
			throw new BusinessException(SchoolErrorCode.IMPORT_FILE_TOO_LARGE,
					"A file can be at most " + MAX_FILE_BYTES / 1024 + " KB",
					Map.of("maxKilobytes", MAX_FILE_BYTES / 1024));
		}
		String content = new String(bytes, StandardCharsets.UTF_8);
		if (!content.isEmpty() && content.charAt(0) == BYTE_ORDER_MARK) {
			content = content.substring(1);
		}
		CsvFile file = CsvFile.parse(content);
		if (file.rows().isEmpty()) {
			throw new BusinessException(SchoolErrorCode.IMPORT_FILE_EMPTY, "The file has a header but no rows");
		}
		Map<String, String> mapping = suggestMapping(importer, file.headers());
		ImportJob job = importJobRepository.save(ImportJob.upload(type, fileName == null ? "upload.csv" : fileName,
				content, file.rows().size(), toJson(mapping), uploadedByUserId));
		return viewOf(job, importer, file, mapping);
	}

	public ImportJobView find(String jobPublicId) {
		ImportJob job = require(jobPublicId);
		RowImporter importer = importerFor(job.getImportType());
		return viewOf(job, importer, CsvFile.parse(job.getContent()), mappingOf(job));
	}

	public Page<ImportJob> list(ImportType type, ImportJobStatus status, Pageable pageable) {
		return importJobRepository.search(TenantContext.getCurrentTenantId(), type, status, pageable);
	}

	/** Replaces the mapping; every required field must be mapped, and only to a column the file has. */
	public ImportJobView remap(String jobPublicId, Map<String, String> mapping) {
		ImportJob job = require(jobPublicId);
		RowImporter importer = importerFor(job.getImportType());
		CsvFile file = CsvFile.parse(job.getContent());
		Map<String, String> cleaned = validated(importer, file, mapping);
		job.remap(toJson(cleaned));
		return viewOf(importJobRepository.save(job), importer, file, cleaned);
	}

	public ImportJobView dryRun(String jobPublicId) {
		ImportJob job = require(jobPublicId);
		job.requireNotCompleted();
		RowImporter importer = importerFor(job.getImportType());
		CsvFile file = CsvFile.parse(job.getContent());
		Map<String, String> mapping = validated(importer, file, mappingOf(job));
		List<RowFailure> failures = importRunner.dryRun(importer, rowsOf(file, mapping));
		replaceReport(job, ImportPhase.DRY_RUN, failures);
		int invalid = ImportRunner.lines(failures).size();
		job.recordDryRun(job.getTotalRows() - invalid, invalid);
		return viewOf(importJobRepository.save(job), importer, file, mapping);
	}

	/** Imports the file once. Lines the last dry run rejected are skipped and reported again. */
	public ImportJobView execute(String jobPublicId) {
		ImportJob unclaimed = require(jobPublicId);
		RowImporter importer = importerFor(unclaimed.getImportType());
		CsvFile file = CsvFile.parse(unclaimed.getContent());
		Map<String, String> mapping = validated(importer, file, mappingOf(unclaimed));
		Map<Integer, RowFailure> rejected = rejectedByDryRun(unclaimed);
		unclaimed.beginImport();
		ImportJob job = importJobRepository.saveAndFlush(unclaimed);
		List<RowFailure> failures = importRunner.run(importer, rowsOf(file, mapping), rejected);
		replaceReport(job, ImportPhase.IMPORT, failures);
		int failed = ImportRunner.lines(failures).size();
		job.recordImport(job.getTotalRows() - failed, failed);
		return viewOf(importJobRepository.save(job), importer, file, mapping);
	}

	public Page<ImportJobError> errors(String jobPublicId, ImportPhase phase, Pageable pageable) {
		ImportJob job = require(jobPublicId);
		return importJobErrorRepository.findByJob(TenantContext.getCurrentTenantId(), job.getId(), phaseOf(job, phase),
				pageable);
	}

	/** The latest report as CSV: the import's once it has run, else the dry run's. */
	public byte[] errorReport(String jobPublicId) {
		ImportJob job = require(jobPublicId);
		StringBuilder csv = new StringBuilder("line,field,message\n");
		for (ImportJobError error : importJobErrorRepository.findAllByJob(TenantContext.getCurrentTenantId(),
				job.getId(), phaseOf(job, null))) {
			csv.append(error.getLineNumber()).append(',').append(quoted(error.getField())).append(',')
					.append(quoted(error.getMessage())).append('\n');
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private Map<Integer, RowFailure> rejectedByDryRun(ImportJob job) {
		if (job.getStatus() != ImportJobStatus.VALIDATED) {
			return Map.of();
		}
		return importJobErrorRepository
				.findAllByJob(TenantContext.getCurrentTenantId(), job.getId(), ImportPhase.DRY_RUN).stream()
				.collect(Collectors.toMap(ImportJobError::getLineNumber,
						error -> new RowFailure(error.getLineNumber(), error.getField(), error.getMessage()),
						(first, other) -> first));
	}

	private void replaceReport(ImportJob job, ImportPhase phase, List<RowFailure> failures) {
		Long tenantId = TenantContext.getCurrentTenantId();
		transactionTemplate.executeWithoutResult(status -> {
			importJobErrorRepository.deleteByJob(tenantId, job.getId(), phase);
			importJobErrorRepository.saveAll(failures.stream()
					.sorted(Comparator.comparingInt(RowFailure::lineNumber))
					.map(failure -> ImportJobError.of(job.getId(), phase, failure.lineNumber(), failure.field(),
							failure.message()))
					.toList());
		});
	}

	private static ImportPhase phaseOf(ImportJob job, ImportPhase requested) {
		if (requested != null) {
			return requested;
		}
		return job.getStatus() == ImportJobStatus.COMPLETED ? ImportPhase.IMPORT : ImportPhase.DRY_RUN;
	}

	// The header is line 1, so the first data row is line 2: the numbers match the file in a spreadsheet.
	private static List<ImportRow> rowsOf(CsvFile file, Map<String, String> mapping) {
		List<ImportRow> rows = new ArrayList<>();
		for (int index = 0; index < file.rows().size(); index++) {
			List<String> values = file.rows().get(index);
			Map<String, String> byField = new LinkedHashMap<>();
			mapping.forEach((field, header) -> {
				String value = file.value(values, header);
				if (value != null) {
					byField.put(field, value);
				}
			});
			rows.add(new ImportRow(index + 2, byField));
		}
		return rows;
	}

	private static Map<String, String> suggestMapping(RowImporter importer, List<String> headers) {
		Map<String, String> byNormalised = headers.stream()
				.collect(Collectors.toMap(ImportService::normalised, Function.identity(), (first, other) -> first));
		Map<String, String> mapping = new LinkedHashMap<>();
		for (ImportField field : importer.fields()) {
			String header = byNormalised.get(normalised(field.name()));
			if (header != null) {
				mapping.put(field.name(), header);
			}
		}
		return mapping;
	}

	private static Map<String, String> validated(RowImporter importer, CsvFile file, Map<String, String> mapping) {
		Map<String, String> cleaned = new LinkedHashMap<>();
		List<String> known = importer.fields().stream().map(ImportField::name).toList();
		mapping.forEach((field, header) -> {
			if (!known.contains(field)) {
				throw new BusinessException(SchoolErrorCode.IMPORT_MAPPING_INVALID,
						"Unknown field " + field + "; expected one of " + known, Map.of("field", field));
			}
			if (header != null && !header.isBlank()) {
				if (!file.headers().contains(header)) {
					throw new BusinessException(SchoolErrorCode.IMPORT_MAPPING_INVALID,
							"The file has no column named " + header, Map.of("column", header));
				}
				cleaned.put(field, header);
			}
		});
		List<String> missing = importer.fields().stream().filter(ImportField::required)
				.map(ImportField::name).filter(field -> !cleaned.containsKey(field)).toList();
		if (!missing.isEmpty()) {
			throw new BusinessException(SchoolErrorCode.IMPORT_MAPPING_INVALID,
					"Map a column to every required field; missing " + missing, Map.of("missing", missing));
		}
		return cleaned;
	}

	private ImportJobView viewOf(ImportJob job, RowImporter importer, CsvFile file, Map<String, String> mapping) {
		List<Map<String, String>> preview = file.rows().stream().limit(PREVIEW_ROWS).map(row -> {
			Map<String, String> byHeader = new LinkedHashMap<>();
			for (String header : file.headers()) {
				String value = file.value(row, header);
				byHeader.put(header, value == null ? "" : value);
			}
			return byHeader;
		}).toList();
		return new ImportJobView(job, file.headers(), importer.fields(), mapping, preview);
	}

	private RowImporter importerFor(ImportType type) {
		RowImporter importer = importers.get(type);
		if (importer == null) {
			throw new BusinessException(PlatformErrorCode.INTERNAL_ERROR, "No importer for " + type);
		}
		if (!permissionAuthorizationService.hasPermission(importer.requiredPermission())) {
			throw new AccessDeniedException("Importing " + type.name().toLowerCase(Locale.ROOT).replace('_', ' ')
					+ " needs the " + importer.requiredPermission() + " permission");
		}
		return importer;
	}

	private ImportJob require(String jobPublicId) {
		return importJobRepository.findByPublicIdAndTenantId(UUID.fromString(jobPublicId),
				TenantContext.getCurrentTenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Import not found: " + jobPublicId));
	}

	private Map<String, String> mappingOf(ImportJob job) {
		return objectMapper.readValue(job.getColumnMapping(), MAPPING);
	}

	private String toJson(Map<String, String> mapping) {
		return objectMapper.writeValueAsString(mapping);
	}

	private static String normalised(String name) {
		return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	// Spreadsheets run a cell that starts with these as a formula; a leading quote keeps it as text.
	private static String quoted(String value) {
		if (value == null) {
			return "";
		}
		String safe = !value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
		return "\"" + safe.replace("\"", "\"\"") + "\"";
	}
}
