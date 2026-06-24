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
import com.altafjava.school.api.dto.request.CreateFeeStructureRequest;

class CreateFeeStructureRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<CreateFeeStructureRequest>> violationsFor(CreateFeeStructureRequest req) {
		return validator.validate(req);
	}

	@Test
	void valid_request_passesAllConstraints() {
		var req = new CreateFeeStructureRequest("Tuition Fee", new BigDecimal("5000.00"), "MONTHLY", "STANDARD");
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void name_blank_failsValidation() {
		var req = new CreateFeeStructureRequest("", new BigDecimal("5000.00"), "MONTHLY", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void name_tooLong_failsValidation() {
		var req = new CreateFeeStructureRequest("N".repeat(201), new BigDecimal("5000.00"), "MONTHLY", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void amount_null_failsValidation() {
		var req = new CreateFeeStructureRequest("Tuition Fee", null, "MONTHLY", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void amount_zero_failsDecimalMinValidation() {
		var req = new CreateFeeStructureRequest("Tuition Fee", BigDecimal.ZERO, "MONTHLY", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void amount_negative_failsDecimalMinValidation() {
		var req = new CreateFeeStructureRequest("Tuition Fee", new BigDecimal("-1.00"), "MONTHLY", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void amount_minimumPositive_passesValidation() {
		var req = new CreateFeeStructureRequest("Tuition Fee", new BigDecimal("0.01"), "MONTHLY", null);
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void frequency_blank_failsValidation() {
		var req = new CreateFeeStructureRequest("Tuition Fee", new BigDecimal("5000.00"), "", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void planType_null_passesValidation() {
		// planType has no @NotNull — it is optional
		var req = new CreateFeeStructureRequest("Tuition Fee", new BigDecimal("5000.00"), "MONTHLY", null);
		assertTrue(violationsFor(req).isEmpty());
	}
}
