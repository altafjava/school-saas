package com.altafjava.school.domain.employee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.domain.teacher.model.Teacher;

class EmployeeTest {

	private static final LocalDate JOINED = LocalDate.of(2020, 1, 1);

	private Employee clerk() {
		return Employee.create(StaffCategory.ADMINISTRATIVE, "EMP-1", "Cara", "Clerk", "cara@school.test", JOINED);
	}

	@Test
	void create_startsActiveInTheGivenCategory() {
		Employee employee = clerk();

		assertTrue(employee.isActive());
		assertEquals(StaffCategory.ADMINISTRATIVE, employee.getStaffCategory());
		assertNull(employee.getExitDate());
	}

	@Test
	void create_rejectsTheTeachingCategory_becauseTeachersAreTeachers() {
		assertThrows(BusinessException.class,
				() -> Employee.create(StaffCategory.TEACHING, "EMP-1", "A", "B", "a@b.test", JOINED));
		assertThrows(BusinessException.class,
				() -> Employee.create(null, "EMP-1", "A", "B", "a@b.test", JOINED));
	}

	@Test
	void teacher_isAnActiveTeachingEmployee() {
		Teacher teacher = Teacher.create("EMP-2", "Jane", "Doe", "jane@school.test", JOINED);

		assertEquals(StaffCategory.TEACHING, teacher.getStaffCategory());
		assertTrue(teacher.isActive());
	}

	@Test
	void exit_recordsStatusDateAndReason() {
		Employee employee = clerk();

		employee.exit(EmployeeStatus.RESIGNED, LocalDate.now(), "Relocating");

		assertFalse(employee.isActive());
		assertEquals(EmployeeStatus.RESIGNED, employee.getStatus());
		assertEquals(LocalDate.now(), employee.getExitDate());
		assertEquals("Relocating", employee.getExitReason());
	}

	@Test
	void exit_isFinal() {
		Employee employee = clerk();
		employee.exit(EmployeeStatus.TERMINATED, LocalDate.now(), null);

		assertThrows(BusinessException.class, () -> employee.exit(EmployeeStatus.RESIGNED, LocalDate.now(), null));
	}

	@Test
	void exit_rejectsFutureOrPreJoinDatesAndNonExitStatuses() {
		Employee employee = clerk();

		assertThrows(BusinessException.class,
				() -> employee.exit(EmployeeStatus.RESIGNED, LocalDate.now().plusDays(1), null));
		assertThrows(BusinessException.class, () -> employee.exit(EmployeeStatus.RESIGNED, JOINED.minusDays(1), null));
		assertThrows(BusinessException.class, () -> employee.exit(EmployeeStatus.ACTIVE, LocalDate.now(), null));
		assertThrows(BusinessException.class, () -> employee.exit(null, LocalDate.now(), null));
		assertThrows(BusinessException.class, () -> employee.exit(EmployeeStatus.RETIRED, null, null));
		assertTrue(employee.isActive());
	}

	@Test
	void probation_isTrackedUntilItsEndDate() {
		Employee employee = clerk();
		employee.setProbationPeriod(LocalDate.of(2026, 6, 30));

		assertTrue(employee.isOnProbation(LocalDate.of(2026, 6, 29)));
		assertFalse(employee.isOnProbation(LocalDate.of(2026, 6, 30)));
		employee.endProbation();
		assertFalse(employee.isOnProbation(LocalDate.of(2026, 1, 1)));
	}

	@Test
	void erasePii_clearsIdentityButKeepsTheEmployeeCode() {
		Employee employee = clerk();
		employee.updatePhone("+14155552671");

		employee.erasePii();

		assertEquals("[erased]", employee.getFirstName());
		assertNull(employee.getPhone());
		assertEquals("EMP-1", employee.getEmployeeCode());
	}
}
