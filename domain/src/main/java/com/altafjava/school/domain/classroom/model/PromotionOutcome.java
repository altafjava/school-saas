package com.altafjava.school.domain.classroom.model;

public enum PromotionOutcome {
	PROMOTE, RETAIN, GRADUATE, WITHDRAW;

	/** Promoted and retained students take a seat in the next academic year; the others leave. */
	public boolean needsClassroom() {
		return this == PROMOTE || this == RETAIN;
	}
}
