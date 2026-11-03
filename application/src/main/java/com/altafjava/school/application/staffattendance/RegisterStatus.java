package com.altafjava.school.application.staffattendance;

/** A staff member's day on the register. {@link #NOT_MARKED} means nothing is recorded yet, not absence. */
public enum RegisterStatus {
	PRESENT, HALF_DAY, ABSENT, ON_LEAVE, NOT_MARKED
}
