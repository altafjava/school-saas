package com.altafjava.school.domain.employee.model;

public enum EmployeeStatus {
	ACTIVE, RESIGNED, TERMINATED, RETIRED;

	public boolean isExit() {
		return this != ACTIVE;
	}
}
