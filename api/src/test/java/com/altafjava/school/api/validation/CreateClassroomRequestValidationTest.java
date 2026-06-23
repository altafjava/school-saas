package com.altafjava.school.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.altafjava.school.api.dto.request.CreateClassroomRequest;

class CreateClassroomRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<CreateClassroomRequest>> violationsFor(CreateClassroomRequest req) {
		return validator.validate(req);
	}

	private CreateClassroomRequest valid() {
		return new CreateClassroomRequest("GR10-A", "Grade 10", "A", "2024-2025", null);
	}

	@Test
	void valid_request_passesAllConstraints() {
		assertTrue(violationsFor(valid()).isEmpty());
	}

	@Test
	void classCode_blank_failsValidation() {
		var req = new CreateClassroomRequest("", "Grade 10", "A", "2024-2025", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void classCode_tooLong_failsValidation() {
		var req = new CreateClassroomRequest("C".repeat(51), "Grade 10", "A", "2024-2025", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void grade_blank_failsValidation() {
		var req = new CreateClassroomRequest("GR10-A", "", "A", "2024-2025", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void grade_tooLong_failsValidation() {
		var req = new CreateClassroomRequest("GR10-A", "G".repeat(21), "A", "2024-2025", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void section_blank_failsValidation() {
		var req = new CreateClassroomRequest("GR10-A", "Grade 10", "", "2024-2025", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void section_tooLong_failsValidation() {
		var req = new CreateClassroomRequest("GR10-A", "Grade 10", "S".repeat(11), "2024-2025", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void academicYear_blank_failsValidation() {
		var req = new CreateClassroomRequest("GR10-A", "Grade 10", "A", "", null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void classTeacherId_null_passesValidation() {
		// classTeacherId has no @NotNull — it is optional
		assertTrue(violationsFor(new CreateClassroomRequest("GR10-A", "Grade 10", "A", "2024-2025", null)).isEmpty());
	}
}
