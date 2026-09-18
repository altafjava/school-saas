package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class FeeAssignmentTest {

	@Test
	void forStudent_setsStudentScope() {
		FeeAssignment assignment = FeeAssignment.forStudent(1L, 2L);

		assertEquals(FeeAssignmentScope.STUDENT, assignment.getScope());
		assertEquals(2L, assignment.getStudentId());
		assertNull(assignment.getClassroomId());
	}

	@Test
	void forClassroom_setsClassroomScope() {
		FeeAssignment assignment = FeeAssignment.forClassroom(1L, 3L);

		assertEquals(FeeAssignmentScope.CLASSROOM, assignment.getScope());
		assertEquals(3L, assignment.getClassroomId());
		assertNull(assignment.getStudentId());
	}

	@Test
	void configureDueDate_setsOverrideFields() {
		FeeAssignment assignment = FeeAssignment.forStudent(1L, 2L);

		assignment.configureDueDate(LocalDate.of(2026, 6, 1), 5, BigDecimal.valueOf(2.5));

		assertEquals(LocalDate.of(2026, 6, 1), assignment.getDueDate());
		assertEquals(5, assignment.getGraceDays());
		assertEquals(BigDecimal.valueOf(2.5), assignment.getLateFeePercentage());
	}

	@Test
	void validateScope_rejectsStudentScopeWithNoStudentId() {
		FeeAssignment assignment = FeeAssignment.builder()
				.feeStructureId(1L)
				.scope(FeeAssignmentScope.STUDENT)
				.build();

		assertThrows(BusinessException.class, () -> invokeValidateScope(assignment));
	}

	@Test
	void validateScope_rejectsStudentScopeWithClassroomIdAlsoSet() {
		FeeAssignment assignment = FeeAssignment.builder()
				.feeStructureId(1L)
				.scope(FeeAssignmentScope.STUDENT)
				.studentId(2L)
				.classroomId(3L)
				.build();

		assertThrows(BusinessException.class, () -> invokeValidateScope(assignment));
	}

	@Test
	void validateScope_rejectsClassroomScopeWithNoClassroomId() {
		FeeAssignment assignment = FeeAssignment.builder()
				.feeStructureId(1L)
				.scope(FeeAssignmentScope.CLASSROOM)
				.build();

		assertThrows(BusinessException.class, () -> invokeValidateScope(assignment));
	}

	@Test
	void validateScope_acceptsCorrectlyScopedAssignments() {
		invokeValidateScope(FeeAssignment.forStudent(1L, 2L));
		invokeValidateScope(FeeAssignment.forClassroom(1L, 3L));
	}

	// validateScope() is a @PrePersist/@PreUpdate lifecycle callback, invoked by Hibernate at
	// flush time rather than from application code — reflection is the only way to exercise it
	// directly in a plain unit test without a real persistence context.
	private void invokeValidateScope(FeeAssignment assignment) {
		try {
			var method = FeeAssignment.class.getDeclaredMethod("validateScope");
			method.setAccessible(true);
			method.invoke(assignment);
		} catch (java.lang.reflect.InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new RuntimeException(e.getCause());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
