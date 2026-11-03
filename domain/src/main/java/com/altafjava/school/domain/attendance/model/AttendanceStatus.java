package com.altafjava.school.domain.attendance.model;

import java.math.BigDecimal;

public enum AttendanceStatus {
	PRESENT(BigDecimal.ONE), ABSENT(BigDecimal.ZERO), LATE(BigDecimal.ONE), EXCUSED(BigDecimal.ZERO), HALF_DAY(
			new BigDecimal("0.5"));

	private final BigDecimal presenceCredit;

	AttendanceStatus(BigDecimal presenceCredit) {
		this.presenceCredit = presenceCredit;
	}

	/** How much of a school day the status counts as attended: a late arrival was in school, a half day half. */
	public BigDecimal presenceCredit() {
		return presenceCredit;
	}
}
