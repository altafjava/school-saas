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
import com.altafjava.school.api.dto.request.CreateTeacherRequest;

class CreateTeacherRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<CreateTeacherRequest>> violationsFor(CreateTeacherRequest req) {
		return validator.validate(req);
	}

	private CreateTeacherRequest valid() {
		return new CreateTeacherRequest("EMP-001", "Bob", "Jones", "bob@school.com", LocalDate.of(2022, 1, 10));
	}

	@Test
	void valid_request_passesAllConstraints() {
		assertTrue(violationsFor(valid()).isEmpty());
	}

	@Test
	void employeeCode_blank_failsValidation() {
		var req = new CreateTeacherRequest("", "Bob", "Jones", "bob@school.com", LocalDate.of(2022, 1, 10));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void employeeCode_tooLong_failsValidation() {
		var req = new CreateTeacherRequest("E".repeat(51), "Bob", "Jones", "bob@school.com", LocalDate.of(2022, 1, 10));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void firstName_blank_failsValidation() {
		var req = new CreateTeacherRequest("EMP-001", "", "Jones", "bob@school.com", LocalDate.of(2022, 1, 10));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void lastName_blank_failsValidation() {
		var req = new CreateTeacherRequest("EMP-001", "Bob", "", "bob@school.com", LocalDate.of(2022, 1, 10));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void email_blank_failsValidation() {
		var req = new CreateTeacherRequest("EMP-001", "Bob", "Jones", "", LocalDate.of(2022, 1, 10));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void email_invalidFormat_failsValidation() {
		var req = new CreateTeacherRequest("EMP-001", "Bob", "Jones", "not-an-email", LocalDate.of(2022, 1, 10));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void joinDate_null_failsValidation() {
		var req = new CreateTeacherRequest("EMP-001", "Bob", "Jones", "bob@school.com", null);
		assertFalse(violationsFor(req).isEmpty());
	}
}
