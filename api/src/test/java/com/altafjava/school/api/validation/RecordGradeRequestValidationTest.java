package com.altafjava.school.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.altafjava.school.api.dto.request.RecordGradeRequest;

class RecordGradeRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<RecordGradeRequest>> violationsFor(RecordGradeRequest req) {
		return validator.validate(req);
	}

	private RecordGradeRequest valid() {
		return new RecordGradeRequest("id-1", "id-10", new BigDecimal("85.50"));
	}

	@Test
	void valid_request_passesAllConstraints() {
		assertTrue(violationsFor(valid()).isEmpty());
	}

	@Test
	void studentId_null_failsValidation() {
		var req = new RecordGradeRequest(null, "id-10", new BigDecimal("85.50"));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void examId_null_failsValidation() {
		var req = new RecordGradeRequest("id-1", null, new BigDecimal("85.50"));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void marks_null_failsValidation() {
		var req = new RecordGradeRequest("id-1", "id-10", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void marks_negative_failsDecimalMinValidation() {
		var req = new RecordGradeRequest("id-1", "id-10", new BigDecimal("-1.0"));
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void marks_zero_passesValidation() {
		// @DecimalMin("0.0") inclusive — zero is valid
		var req = new RecordGradeRequest("id-1", "id-10", BigDecimal.ZERO);
		assertTrue(violationsFor(req).isEmpty());
	}

}
