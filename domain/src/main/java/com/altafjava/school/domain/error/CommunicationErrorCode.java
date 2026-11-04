package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/** Error codes for announcements and messaging; each is part of the public API contract. */
public enum CommunicationErrorCode implements ErrorCode {

	ANNOUNCEMENT_NOT_DRAFT(409, "Only a draft announcement can be deleted; cancel a scheduled one instead."), //
	AUDIENCE_EMPTY(409, "Nobody in this audience has an account to receive it."), //
	AUDIENCE_CLASSROOM_NOT_CURRENT(400, "Choose classrooms of the current academic year."), //
	AUDIENCE_ROLE_UNKNOWN(400, "The audience names a role that does not exist."), //
	RECIPIENT_HAS_NO_ACCOUNT(409, "The other person has no account yet."), //
	CONVERSATION_JUST_STARTED(409, "The conversation was just started; open it and reply."), //
	REPORT_OWN_MESSAGE(400, "You cannot report your own message."), //
	MESSAGE_ALREADY_REPORTED(409, "You have already reported this message."), //
	CONVERSATION_GUARDIAN_REQUIRED(400, "Name the guardian to message."), //
	CONVERSATION_TEACHER_REQUIRED(400, "Name the teacher to message."), //
	MESSAGING_HOURS_INCOMPLETE(400, "Set both messaging hours or neither."), //
	MESSAGING_DISABLED(403, "Messaging is switched off at this school."), //
	MESSAGING_CLOSED(403, "Messaging is outside its open hours."), //
	AUDIENCE_TARGETS_REQUIRED(400, "Choose at least one target for this audience."), //
	CHANNELS_REQUIRED(400, "Choose at least one delivery channel."), //
	PUBLISH_TIME_NOT_IN_FUTURE(400, "The publish time must be in the future."), //
	ANNOUNCEMENT_NOT_EDITABLE(409, "Only a draft or scheduled announcement can be changed."), //
	ANNOUNCEMENT_ALREADY_EXPIRED(400, "The announcement would already have expired."), //
	REPORT_ALREADY_RESOLVED(409, "The report is already resolved.");

	private final int httpStatus;
	private final String description;

	CommunicationErrorCode(int httpStatus, String description) {
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
