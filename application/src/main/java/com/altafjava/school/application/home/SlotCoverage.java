package com.altafjava.school.application.home;

/** Who stands in front of the class for a slot today. */
public enum SlotCoverage {
	/** The timetabled teacher. */
	REGULAR,
	/** Someone else covers the timetabled teacher; the slot names the substitute. */
	SUBSTITUTED,
	/** The teacher reading the slot covers it for a colleague. */
	COVERING
}
