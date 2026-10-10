package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.application.student.BulkImportResult;
import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.student.model.Student;

@ExtendWith(MockitoExtension.class)
class StudentBulkImportServiceTest {

	@Mock
	private StudentService studentService;

	private StudentBulkImportService bulkImportService;

	private void setUp() {
		bulkImportService = new StudentBulkImportService(studentService);
	}

	private InputStream csv(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void importCsv_withAllValidRows_enrollsEachAndReportsNoFailures() {
		setUp();
		String content = """
				studentCode,firstName,lastName,email,dateOfBirth
				STU-001,Alice,Smith,alice@school.test,2010-01-15
				STU-002,Bob,Jones,bob@school.test,2011-02-20
				""";
		when(studentService.enroll(any(), any(), any(), any(), any(), any(), any())).thenReturn(new Student());

		BulkImportResult result = bulkImportService.importCsv(csv(content));

		assertEquals(2, result.totalRows());
		assertEquals(2, result.successCount());
		assertEquals(0, result.failureCount());
		verify(studentService).enroll("STU-001", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 15),
				Gender.NOT_SPECIFIED, null);
		verify(studentService).enroll("STU-002", "Bob", "Jones", "bob@school.test", LocalDate.of(2011, 2, 20),
				Gender.NOT_SPECIFIED, null);
	}

	@Test
	void importCsv_withBlankOptionalFields_treatsThemAsNull() {
		setUp();
		String content = """
				studentCode,firstName,lastName,email,dateOfBirth
				STU-001,Alice,Smith,,
				""";
		when(studentService.enroll(any(), any(), any(), any(), any(), any(), any())).thenReturn(new Student());

		BulkImportResult result = bulkImportService.importCsv(csv(content));

		assertEquals(1, result.successCount());
		verify(studentService).enroll(eq("STU-001"), eq("Alice"), eq("Smith"), isNull(), isNull(),
				eq(Gender.NOT_SPECIFIED),
				isNull());
	}

	@Test
	void importCsv_withDuplicateStudentCode_reportsThatRowButContinuesWithOthers() {
		setUp();
		String content = """
				studentCode,firstName,lastName,email,dateOfBirth
				STU-001,Alice,Smith,alice@school.test,2010-01-15
				STU-002,Bob,Jones,bob@school.test,2011-02-20
				""";
		when(studentService.enroll(eq("STU-001"), any(), any(), any(), any(), any(), any()))
				.thenThrow(new BusinessException("Student code already exists: STU-001"));
		when(studentService.enroll(eq("STU-002"), any(), any(), any(), any(), any(), any())).thenReturn(new Student());

		BulkImportResult result = bulkImportService.importCsv(csv(content));

		assertEquals(2, result.totalRows());
		assertEquals(1, result.successCount());
		assertEquals(1, result.failureCount());
		assertEquals(2, result.failures().get(0).rowNumber());
		assertEquals("STU-001", result.failures().get(0).studentCode());
		assertTrue(result.failures().get(0).error().contains("already exists"));
		verify(studentService).enroll(eq("STU-002"), any(), any(), any(), any(), any(), any());
	}

	@Test
	void importCsv_withMissingRequiredField_reportsRowFailureWithoutCallingEnroll() {
		setUp();
		String content = """
				studentCode,firstName,lastName,email,dateOfBirth
				STU-001,,Smith,alice@school.test,2010-01-15
				""";

		BulkImportResult result = bulkImportService.importCsv(csv(content));

		assertEquals(1, result.failureCount());
		assertTrue(result.failures().get(0).error().contains("firstName"));
		verify(studentService, org.mockito.Mockito.never()).enroll(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void importCsv_withMalformedDate_reportsRowFailure() {
		setUp();
		String content = """
				studentCode,firstName,lastName,email,dateOfBirth
				STU-001,Alice,Smith,alice@school.test,not-a-date
				""";

		BulkImportResult result = bulkImportService.importCsv(csv(content));

		assertEquals(1, result.failureCount());
		assertEquals("STU-001", result.failures().get(0).studentCode());
		verify(studentService, org.mockito.Mockito.never()).enroll(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void importCsv_withEmptyFile_returnsZeroRowsNoFailures() {
		setUp();
		String content = "studentCode,firstName,lastName,email,dateOfBirth\n";

		BulkImportResult result = bulkImportService.importCsv(csv(content));

		assertEquals(0, result.totalRows());
		assertEquals(0, result.successCount());
		assertEquals(0, result.failureCount());
	}

	@Test
	void importCsv_withGenderColumn_passesTheParsedGender() {
		setUp();
		String content = """
				studentCode,firstName,lastName,email,dateOfBirth,gender
				STU-001,Alice,Smith,,,female
				STU-002,Bob,Jones,,,
				""";
		when(studentService.enroll(any(), any(), any(), any(), any(), any(), any())).thenReturn(new Student());

		BulkImportResult result = bulkImportService.importCsv(csv(content));

		assertEquals(2, result.successCount());
		verify(studentService).enroll(eq("STU-001"), any(), any(), isNull(), isNull(), eq(Gender.FEMALE), isNull());
		verify(studentService).enroll(eq("STU-002"), any(), any(), isNull(), isNull(), eq(Gender.NOT_SPECIFIED),
				isNull());
	}

	@Test
	void importCsv_withUnknownGender_reportsRowFailureWithoutCallingEnroll() {
		setUp();
		String content = """
				studentCode,firstName,lastName,email,dateOfBirth,gender
				STU-001,Alice,Smith,,,robot
				""";

		BulkImportResult result = bulkImportService.importCsv(csv(content));

		assertEquals(1, result.failureCount());
		assertTrue(result.failures().get(0).error().contains("gender must be"));
		verify(studentService, org.mockito.Mockito.never()).enroll(any(), any(), any(), any(), any(), any(), any());
	}
}
