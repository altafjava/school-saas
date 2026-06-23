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
import com.altafjava.school.api.dto.request.RecordFeePaymentRequest;

class RecordFeePaymentRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<RecordFeePaymentRequest>> violationsFor(RecordFeePaymentRequest req) {
		return validator.validate(req);
	}

	private RecordFeePaymentRequest valid() {
		return new RecordFeePaymentRequest(1L, 5L, new BigDecimal("5000.00"),
				LocalDateTime.of(2024, 9, 1, 10, 0), "REC-2024-001");
	}

	@Test
	void valid_request_passesAllConstraints() {
		assertTrue(violationsFor(valid()).isEmpty());
	}

	@Test
	void studentId_null_failsValidation() {
		var req = new RecordFeePaymentRequest(null, 5L, new BigDecimal("5000.00"),
				LocalDateTime.of(2024, 9, 1, 10, 0), "REC-2024-001");
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void feeStructureId_null_failsValidation() {
		var req = new RecordFeePaymentRequest(1L, null, new BigDecimal("5000.00"),
				LocalDateTime.of(2024, 9, 1, 10, 0), "REC-2024-001");
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void paidAmount_null_failsValidation() {
		var req = new RecordFeePaymentRequest(1L, 5L, null, LocalDateTime.of(2024, 9, 1, 10, 0), "REC-2024-001");
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void paidAmount_zero_failsDecimalMinValidation() {
		var req = new RecordFeePaymentRequest(1L, 5L, BigDecimal.ZERO,
				LocalDateTime.of(2024, 9, 1, 10, 0), "REC-2024-001");
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void paidAmount_negative_failsDecimalMinValidation() {
		var req = new RecordFeePaymentRequest(1L, 5L, new BigDecimal("-100.00"),
				LocalDateTime.of(2024, 9, 1, 10, 0), "REC-2024-001");
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void paidAmount_minimumPositive_passesValidation() {
		var req = new RecordFeePaymentRequest(1L, 5L, new BigDecimal("0.01"),
				LocalDateTime.of(2024, 9, 1, 10, 0), "REC-2024-001");
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void paidAt_null_failsValidation() {
		var req = new RecordFeePaymentRequest(1L, 5L, new BigDecimal("5000.00"), null, "REC-2024-001");
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void receiptNumber_blank_failsValidation() {
		var req = new RecordFeePaymentRequest(1L, 5L, new BigDecimal("5000.00"),
				LocalDateTime.of(2024, 9, 1, 10, 0), "");
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void receiptNumber_tooLong_failsValidation() {
		var req = new RecordFeePaymentRequest(1L, 5L, new BigDecimal("5000.00"),
				LocalDateTime.of(2024, 9, 1, 10, 0), "R".repeat(101));
		assertFalse(violationsFor(req).isEmpty());
	}
}
