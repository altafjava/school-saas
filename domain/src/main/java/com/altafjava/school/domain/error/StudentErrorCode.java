package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/** Error codes for students, guardians and their records; each is part of the public API contract. */
public enum StudentErrorCode implements ErrorCode {

	LIFECYCLE_DATE_IN_FUTURE(400, "A lifecycle change cannot take effect in the future."), //
	ALUMNI_NOT_GRADUATED(409, "Only a graduated student can have an alumni profile."), //
	CERTIFICATE_TYPE_INACTIVE(409, "The certificate type is not active."), //
	GUARDIAN_NOT_INVITED(409, "The school has no pending guardian record for this email."), //
	STUDENT_LEAVE_OVERLAPS(409, "A leave request already covers these dates."), //
	STUDENT_CODE_TAKEN(409, "Another student already uses this student code."), //
	SIBLING_SELF(400, "A student cannot be their own sibling."), //
	SIBLING_EXISTS(409, "They are already recorded as siblings."), //
	SIBLING_NONE(409, "The student has no recorded siblings."), //
	ACCOUNT_EXISTS(409, "This record already has a login."), //
	ACCOUNT_LINKED_ELSEWHERE(409, "This login already belongs to another record."), //
	CERTIFICATE_TYPE_CODE_INVALID(400, "The code must be 2-50 characters of A-Z, 0-9 and _, starting with a letter."), //
	DOCUMENT_REJECTION_REASON_REQUIRED(400, "A rejection reason is required."), //
	DOCUMENT_ALREADY_VERIFIED(409, "The document has already been reviewed; upload a new one instead."), //
	EMERGENCY_CONTACT_PRIORITY_INVALID(400, "Emergency contact priority must be 1 or greater."), //
	CONSENT_NOT_GRANTED(409, "The consent is not currently granted."), //
	PICKUP_CUSTODY_RESTRICTED(409, "Pickup cannot be authorized while custody is restricted."), //
	STUDENT_LEAVE_DATES_INVALID(400, "The leave ends before it starts."), //
	STUDENT_LEAVE_STATUS_INVALID(409, "The student leave request is not in a status that allows this action."), //
	STUDENT_STATUS_INVALID(409, "The student's enrollment status does not allow this action.");

	private final int httpStatus;
	private final String description;

	StudentErrorCode(int httpStatus, String description) {
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
