package com.altafjava.school.domain.guardian.model;

/** Outcome of "may this adult collect this child?" — anything but {@link #AUTHORIZED} means no. */
public enum PickupDecision {
	AUTHORIZED, NOT_AUTHORIZED, CUSTODY_RESTRICTED, NOT_LINKED
}
