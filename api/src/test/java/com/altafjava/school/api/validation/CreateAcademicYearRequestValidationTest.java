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
import com.altafjava.school.api.dto.request.CreateAcademicYearRequest;

class CreateAcademicYearRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<CreateAcademicYearRequest>> violationsFor(CreateAcademicYearRequest req) {
		return validator.validate(req);
	}

	@Test
	void valid_request_passesAllConstraints() {
		var req = new CreateAcademicYearRequest("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31),
				false);
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void name_blank_failsValidation() {
		var req = new CreateAcademicYearRequest("", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void name_tooLong_failsValidation() {
		var req = new CreateAcademicYearRequest("A".repeat(51), LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31),
				false);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void startDate_null_failsValidation() {
		var req = new CreateAcademicYearRequest("2024-2025", null, LocalDate.of(2025, 5, 31), false);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void endDate_null_failsValidation() {
		var req = new CreateAcademicYearRequest("2024-2025", LocalDate.of(2024, 6, 1), null, false);
		assertFalse(violationsFor(req).isEmpty());
	}
}
