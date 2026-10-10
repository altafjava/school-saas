package com.altafjava.school.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.altafjava.school.api.dto.request.ScheduleExamRequest;

class ScheduleExamRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<ScheduleExamRequest>> violationsFor(ScheduleExamRequest req) {
		return validator.validate(req);
	}

	private ScheduleExamRequest valid() {
		return new ScheduleExamRequest("Midterm Exam", "id-5", "id-10",
				LocalDateTime.of(2025, 10, 15, 9, 0), new BigDecimal("100.0"), null, "id-1", null);
	}

	@Test
	void valid_request_passesAllConstraints() {
		assertTrue(violationsFor(valid()).isEmpty());
	}

	@Test
	void title_blank_failsValidation() {
		var req = new ScheduleExamRequest("", "id-5", "id-10",
				LocalDateTime.of(2025, 10, 15, 9, 0), new BigDecimal("100.0"), null, "id-1", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void title_tooLong_failsValidation() {
		var req = new ScheduleExamRequest("T".repeat(201), "id-5", "id-10",
				LocalDateTime.of(2025, 10, 15, 9, 0), new BigDecimal("100.0"), null, "id-1", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void subjectId_null_failsValidation() {
		var req = new ScheduleExamRequest("Midterm Exam", null, "id-10",
				LocalDateTime.of(2025, 10, 15, 9, 0), new BigDecimal("100.0"), null, "id-1", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void classroomId_null_failsValidation() {
		var req = new ScheduleExamRequest("Midterm Exam", "id-5", null,
				LocalDateTime.of(2025, 10, 15, 9, 0), new BigDecimal("100.0"), null, "id-1", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void scheduledAt_null_failsValidation() {
		var req = new ScheduleExamRequest("Midterm Exam", "id-5", "id-10", null, new BigDecimal("100.0"), null,
				"id-1", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void maxMarks_null_failsValidation() {
		var req = new ScheduleExamRequest("Midterm Exam", "id-5", "id-10",
				LocalDateTime.of(2025, 10, 15, 9, 0), null, null, "id-1", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void maxMarks_belowMin_failsDecimalMinValidation() {
		// @DecimalMin("1.0") — zero should fail
		var req = new ScheduleExamRequest("Midterm Exam", "id-5", "id-10",
				LocalDateTime.of(2025, 10, 15, 9, 0), BigDecimal.ZERO, null, "id-1", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void maxMarks_atMinimum_passesValidation() {
		var req = new ScheduleExamRequest("Midterm Exam", "id-5", "id-10",
				LocalDateTime.of(2025, 10, 15, 9, 0), new BigDecimal("1.0"), null, "id-1", null);
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void examType_null_failsValidation() {
		var req = new ScheduleExamRequest("Midterm Exam", "id-5", "id-10",
				LocalDateTime.of(2025, 10, 15, 9, 0), new BigDecimal("100.0"), null, null, null);
		assertFalse(violationsFor(req).isEmpty());
	}
}
