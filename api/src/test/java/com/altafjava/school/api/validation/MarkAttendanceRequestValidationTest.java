package com.altafjava.school.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.altafjava.school.api.dto.request.MarkAttendanceRequest;

class MarkAttendanceRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<MarkAttendanceRequest>> violationsFor(MarkAttendanceRequest req) {
		return validator.validate(req);
	}

	@Test
	void valid_request_passesAllConstraints() {
		var req = new MarkAttendanceRequest(1L, 10L, LocalDate.of(2024, 9, 1), "PRESENT", "teacher@school.com");
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void studentId_null_failsValidation() {
		var req = new MarkAttendanceRequest(null, 10L, LocalDate.of(2024, 9, 1), "PRESENT", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void classroomId_null_failsValidation() {
		var req = new MarkAttendanceRequest(1L, null, LocalDate.of(2024, 9, 1), "PRESENT", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void attendanceDate_null_failsValidation() {
		var req = new MarkAttendanceRequest(1L, 10L, null, "PRESENT", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void status_blank_failsValidation() {
		var req = new MarkAttendanceRequest(1L, 10L, LocalDate.of(2024, 9, 1), "", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void markedBy_null_passesValidation() {
		// markedBy has no @NotNull — it is optional
		var req = new MarkAttendanceRequest(1L, 10L, LocalDate.of(2024, 9, 1), "PRESENT", null);
		assertTrue(violationsFor(req).isEmpty());
	}
}
