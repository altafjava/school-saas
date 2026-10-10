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
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

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
		var req = new MarkAttendanceRequest("id-1", "id-10", LocalDate.of(2024, 9, 1), AttendanceStatus.PRESENT);
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void studentId_null_failsValidation() {
		var req = new MarkAttendanceRequest(null, "id-10", LocalDate.of(2024, 9, 1), AttendanceStatus.PRESENT);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void classroomId_null_failsValidation() {
		var req = new MarkAttendanceRequest("id-1", null, LocalDate.of(2024, 9, 1), AttendanceStatus.PRESENT);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void attendanceDate_null_failsValidation() {
		var req = new MarkAttendanceRequest("id-1", "id-10", null, AttendanceStatus.PRESENT);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void status_null_failsValidation() {
		var req = new MarkAttendanceRequest("id-1", "id-10", LocalDate.of(2024, 9, 1), null);
		assertFalse(violationsFor(req).isEmpty());
	}

}
