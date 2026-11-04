package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/** Error codes for student and staff attendance; each is part of the public API contract. */
public enum AttendanceErrorCode implements ErrorCode {

	ID_CARD_INVALID(400, "The scanned code is not a valid ID card."), //
	SCHOOL_HOLIDAY(409, "The day is a school holiday."), //
	STUDENT_NOT_ENROLLED(409, "The student is not enrolled in a classroom."), //
	ID_CARD_NOT_STUDENT(400, "Only a student ID card can mark student attendance."), //
	ID_CARD_REVOKED(409, "The ID card has been revoked."), //
	ATTENDANCE_ALREADY_RECORDED(409, "Today's attendance is already recorded."), //
	CHECK_IN_REQUIRED(409, "Check in before checking out."), //
	ATTENDANCE_DATE_IN_FUTURE(400, "Attendance cannot be recorded for a future day."), //
	ON_APPROVED_LEAVE(409, "Approved leave covers this day; cancel the leave to record attendance."), //
	ALREADY_CHECKED_OUT(409, "Already checked out today."), //
	CHECK_TIMES_INVALID(400, "Check-in and check-out must fall on the day, with check-out after check-in."), //
	ATTENDANCE_STATUS_REQUIRED(400, "An attendance status is required."), //
	ABSENT_WITH_TIMES(400, "An absent day has no check-in or check-out."), //
	WORKDAY_INVALID(400, "The workday must start before it ends, with non-negative grace minutes.");

	private final int httpStatus;
	private final String description;

	AttendanceErrorCode(int httpStatus, String description) {
		this.httpStatus = httpStatus;
		this.description = description;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public int httpStatus() {
		return httpStatus;
	}

	@Override
	public String description() {
		return description;
	}
}
