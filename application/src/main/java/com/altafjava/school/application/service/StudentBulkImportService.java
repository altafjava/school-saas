package com.altafjava.school.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import com.altafjava.school.application.student.BulkImportResult;
import com.altafjava.school.application.student.BulkImportResult.RowFailure;
import com.altafjava.school.domain.common.model.Gender;

/**
 * Bulk student-roster CSV import (ROADMAP.md Phase 3) — the first bulk-write path in school-saas.
 * Expected columns: {@code studentCode,firstName,lastName,email,dateOfBirth[,gender]} (header required;
 * {@code email}/{@code dateOfBirth} may be blank, {@code dateOfBirth} in ISO-8601 format; the optional
 * {@code gender} is {@code MALE}, {@code FEMALE}, {@code OTHER} or blank for not specified).
 *
 * <p>
 * Each row is created via {@link StudentService#enroll} — the same validated, tested entry point
 * a single manual enrollment goes through — and each row's outcome is independent: one bad row
 * (duplicate code, malformed date, missing required field) is reported and skipped, not allowed
 * to fail the whole file, per ROADMAP.md's "batched, validated, reported per-row failure"
 * requirement. Rows are not batched into one transaction for the same reason — see
 * StudentService#enroll's own transactional boundary, which commits per row.
 *
 * <p>
 * Deliberately not {@code @QuotaCheck}-gated: {@code QuotaService.getEffectiveLimit} treats a
 * metric with no {@code UsageMetric} row configured as limit {@code 0} (blocked), not unlimited —
 * confirmed by a genuinely failing integration test. Since BULK_IMPORTS is a metric no existing
 * plan has ever needed a limit for, gating on it would make this endpoint reject every tenant
 * outright rather than rate-limit real overuse. Real fix is seeding a real default BULK_IMPORTS
 * limit per plan — a platform-data change outside this phase's scope; flagging it here rather
 * than shipping a quota check that silently breaks the feature it's attached to.
 */
@Service
public class StudentBulkImportService {

	private static final List<String> HEADERS = List.of("studentCode", "firstName", "lastName", "email",
			"dateOfBirth", "gender");
	private static final String UNREADABLE_ROW = "<unreadable row>";

	private final StudentService studentService;

	public StudentBulkImportService(StudentService studentService) {
		this.studentService = studentService;
	}

	public BulkImportResult importCsv(InputStream csvInputStream) {
		CSVFormat format = CSVFormat.DEFAULT.builder()
				.setHeader(HEADERS.toArray(new String[0]))
				.setSkipHeaderRecord(true)
				.setTrim(true)
				.setIgnoreEmptyLines(true)
				.get();

		int totalRows = 0;
		int successCount = 0;
		List<RowFailure> failures = new ArrayList<>();

		try (var reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
				CSVParser parser = CSVParser.builder().setReader(reader).setFormat(format).get()) {
			int rowNumber = 1;
			for (CSVRecord record : parser) {
				rowNumber++;
				totalRows++;
				String studentCode = safeGet(record, "studentCode");
				try {
					enrollFromRow(record);
					successCount++;
				} catch (RuntimeException ex) {
					failures.add(new RowFailure(rowNumber, studentCode == null ? UNREADABLE_ROW : studentCode,
							ex.getMessage()));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read CSV file", e);
		}

		return new BulkImportResult(totalRows, successCount, failures.size(), failures);
	}

	private void enrollFromRow(CSVRecord record) {
		String studentCode = requireNonBlank(record, "studentCode");
		String firstName = requireNonBlank(record, "firstName");
		String lastName = requireNonBlank(record, "lastName");
		String email = blankToNull(safeGet(record, "email"));
		LocalDate dateOfBirth = parseDateOrNull(safeGet(record, "dateOfBirth"));
		Gender gender = parseGender(safeGet(record, "gender"));
		studentService.enroll(studentCode, firstName, lastName, email, dateOfBirth, gender, null);
	}

	private Gender parseGender(String value) {
		if (value == null || value.isBlank()) {
			return Gender.NOT_SPECIFIED;
		}
		try {
			return Gender.valueOf(value.strip().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("gender must be MALE, FEMALE, OTHER or blank, was " + value);
		}
	}

	private String requireNonBlank(CSVRecord record, String column) {
		String value = safeGet(record, column);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(column + " is required");
		}
		return value;
	}

	// A short/malformed row (fewer values than the header) makes record.get() throw — treated as
	// "this column is missing," not as a reason to abort the whole file.
	private String safeGet(CSVRecord record, String column) {
		try {
			return record.isSet(column) ? record.get(column) : null;
		} catch (RuntimeException ex) {
			return null;
		}
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	private LocalDate parseDateOrNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return LocalDate.parse(value);
	}
}
