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
import com.altafjava.school.api.dto.request.CreateStudentRequest;

class CreateStudentRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<CreateStudentRequest>> violationsFor(CreateStudentRequest req) {
		return validator.validate(req);
	}

	private CreateStudentRequest valid() {
		return new CreateStudentRequest("STU-001", "Alice", "Smith", "alice@school.com", LocalDate.of(2010, 5, 15));
	}

	@Test
	void valid_request_passesAllConstraints() {
		assertTrue(violationsFor(valid()).isEmpty());
	}

	@Test
	void studentCode_blank_failsValidation() {
		var req = new CreateStudentRequest("", "Alice", "Smith", "alice@school.com", LocalDate.of(2010, 5, 15));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void studentCode_tooLong_failsValidation() {
		String longCode = "S".repeat(51);
		var req = new CreateStudentRequest(longCode, "Alice", "Smith", "alice@school.com", LocalDate.of(2010, 5, 15));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void firstName_blank_failsValidation() {
		var req = new CreateStudentRequest("STU-001", "", "Smith", "alice@school.com", LocalDate.of(2010, 5, 15));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void firstName_tooLong_failsValidation() {
		var req = new CreateStudentRequest("STU-001", "A".repeat(101), "Smith", "alice@school.com",
				LocalDate.of(2010, 5, 15));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void lastName_blank_failsValidation() {
		var req = new CreateStudentRequest("STU-001", "Alice", "", "alice@school.com", LocalDate.of(2010, 5, 15));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void email_invalidFormat_failsValidation() {
		var req = new CreateStudentRequest("STU-001", "Alice", "Smith", "not-an-email", LocalDate.of(2010, 5, 15));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void email_null_passesValidation() {
		// email has no @NotNull — optional field
		var req = new CreateStudentRequest("STU-001", "Alice", "Smith", null, LocalDate.of(2010, 5, 15));
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void dateOfBirth_null_failsValidation() {
		var req = new CreateStudentRequest("STU-001", "Alice", "Smith", "alice@school.com", null);
		assertFalse(violationsFor(req).isEmpty());
	}
}
