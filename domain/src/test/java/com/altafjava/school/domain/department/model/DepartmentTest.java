package com.altafjava.school.domain.department.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DepartmentTest {

	@Test
	void create_isActiveByDefault() {
		Department department = Department.create("Science", "SCI", "Science department");

		assertTrue(department.isActive());
		assertEquals("Science", department.getName());
	}

	@Test
	void updateDetails_replacesMutableFields() {
		Department department = Department.create("Science", "SCI", null);

		department.updateDetails("Sciences", "SCI2", "Renamed");

		assertEquals("Sciences", department.getName());
		assertEquals("SCI2", department.getCode());
		assertEquals("Renamed", department.getDescription());
	}

	@Test
	void assignHeadEmployee_setsHeadEmployeeId() {
		Department department = Department.create("Science", "SCI", null);

		department.assignHeadEmployee(42L);

		assertEquals(42L, department.getHeadEmployeeId());
	}

	@Test
	void deactivate_thenActivate_flipsFlag() {
		Department department = Department.create("Science", "SCI", null);

		department.deactivate();
		assertFalse(department.isActive());

		department.activate();
		assertTrue(department.isActive());
	}
}
