package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/** Error codes for enquiries and admissions; each is part of the public API contract. */
public enum AdmissionErrorCode implements ErrorCode {

	STUDENT_CODE_REQUIRED(400, "Approving an application needs the new student's code."), //
	APPLICATION_FEE_INVALID(400, "The application fee must be positive with at most 2 decimals."), //
	ENQUIRY_NOT_OPEN(409, "Only an open enquiry can be converted into an application."), //
	ENQUIRY_OWNER_INACTIVE(400, "Only an active staff member can follow up an enquiry."), //
	ADMISSION_STATUS_INVALID(409, "The application is not in a status that allows this step."), //
	APPLICATION_FEE_UNPAID(409, "The application fee must be paid or waived first."), //
	APPLICATION_FEE_STATUS_INVALID(409, "The application fee is not in a status that allows this."), //
	MERIT_RANK_INVALID(400, "The merit rank must be a positive whole number."), //
	ENQUIRY_CONTACT_REQUIRED(400, "An enquiry needs a phone number or an email address."), //
	ENQUIRY_STATUS_INVALID(409, "The enquiry is not in a status that allows this action.");

	private final int httpStatus;
	private final String description;

	AdmissionErrorCode(int httpStatus, String description) {
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
