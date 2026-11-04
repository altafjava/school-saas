package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/**
 * Error codes for imports, custom fields, events, visitors and other school-wide rules; each is part of the public API
 * contract.
 */
public enum SchoolErrorCode implements ErrorCode {

	SORT_INVALID(400, "The sort parameter is not valid; see the message."), //
	IMPORT_TOO_MANY_ROWS(400, "The file has more rows than allowed."), //
	IMPORT_FILE_EMPTY(400, "The file is empty or has a header but no rows."), //
	IMPORT_FILE_INVALID(400, "The file is not valid CSV."), //
	IMPORT_FILE_TOO_LARGE(413, "The file is larger than allowed."), //
	IMPORT_MAPPING_INVALID(400, "The column mapping is not valid; see the message."), //
	DATE_RANGE_INVALID(400, "The end date is before the start date."), //
	DATE_RANGE_TOO_LONG(400, "The period is longer than allowed."), //
	CODE_TAKEN(409, "Another record already uses this code or name."), //
	CUSTOM_FIELD_EXISTS(409, "A custom field with this key already exists."), //
	CUSTOM_FIELD_OPTIONS_REQUIRED(400, "A select field needs at least one option."), //
	CUSTOM_FIELD_REQUIRED(400, "The custom field is required."), //
	CUSTOM_FIELD_UNKNOWN(400, "The custom field does not exist or is not active."), //
	CUSTOM_FIELD_TYPE_MISMATCH(400, "The value does not match the custom field's type."), //
	CUSTOM_FIELD_BELOW_MINIMUM(400, "The value is below the custom field's minimum."), //
	CUSTOM_FIELD_ABOVE_MAXIMUM(400, "The value is above the custom field's maximum."), //
	CUSTOM_FIELD_PATTERN_MISMATCH(400, "The value does not match the custom field's pattern."), //
	CUSTOM_FIELD_OPTION_INVALID(400, "The value is not one of the custom field's options."), //
	EVENT_ALREADY_REGISTERED(409, "The student is already registered for this event."), //
	EVENT_FULL(409, "The event has reached its registration capacity."), //
	VISITOR_PHOTO_REQUIRED(400, "A photo of the visitor is required to check them in."), //
	VISITOR_HOST_LEFT(409, "The host is no longer at the school."), //
	EVENT_REGISTRATION_STATUS_INVALID(409, "The registration is not in a status that allows this action."), //
	TICKET_STATUS_INVALID(409, "The ticket is not in a status that allows this action."), //
	IMPORT_ALREADY_COMPLETED(409, "The file has already been imported; upload it again to import more."), //
	VISITOR_ALREADY_CHECKED_OUT(409, "The visitor has already checked out."), //
	VISITOR_CHECK_OUT_INVALID(400, "Check-out time cannot be before check-in time."), //
	VISIT_DATE_PASSED(400, "The visit date has passed."), //
	VISITOR_REQUEST_STATUS_INVALID(409, "The visitor request is not in a status that allows this action."), //
	VISIT_NOT_TODAY(409, "The visit is approved for another day."), //
	IMPORT_ROW_INVALID(400, "A row of the import file is not valid; see the message.");

	private final int httpStatus;
	private final String description;

	SchoolErrorCode(int httpStatus, String description) {
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
