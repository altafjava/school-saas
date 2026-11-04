package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/** Error codes for employees and leave; each is part of the public API contract. */
public enum StaffErrorCode implements ErrorCode {

	EMPLOYEE_CODE_TAKEN(409, "Another staff member already uses this employee code."), //
	EMPLOYEE_HAS_LEFT(409, "The staff member has left the school."), //
	TEACHER_HAS_CLASSES(409, "Reassign the teacher's classes before recording their exit."), //
	LEAVE_TYPE_UNAVAILABLE(409, "The leave type is not available."), //
	LEAVE_OVERLAPS(409, "A leave request already covers some of these days."), //
	LEAVE_APPROVER_REPEATED(409, "Each approval level must come from a different person."), //
	LEAVE_BALANCE_MISSING(409, "No leave balance is allocated for this leave type."), //
	LEAVE_OVERLAPS_ATTENDANCE(409, "The staff member was at work on a day this leave covers."), //
	LEAVE_REQUEST_STATUS_INVALID(409, "The leave request is not in a status that allows this action."), //
	LEAVE_BALANCE_INSUFFICIENT(409, "The leave balance is too low."), //
	STAFF_ROLE_REQUIRED(400, "A staff login needs a staff role."), //
	TEACHER_CATEGORY_NOT_ALLOWED(400, "Teaching staff are hired as teachers; use a non-teaching category here."), //
	EXIT_REASON_INVALID(400, "An exit must be RESIGNED, TERMINATED or RETIRED."), //
	EXIT_DATE_INVALID(400, "The exit date must be between the join date and today."), //
	LEAVE_DATES_INVALID(400, "The leave ends before it starts."), //
	LEAVE_ALREADY_STARTED(409, "A leave that has already started cannot be cancelled."), //
	LEAVE_APPROVAL_LEVELS_INVALID(400, "A leave type needs one or two levels of approval.");

	private final int httpStatus;
	private final String description;

	StaffErrorCode(int httpStatus, String description) {
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
