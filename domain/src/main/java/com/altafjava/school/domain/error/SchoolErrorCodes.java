package com.altafjava.school.domain.error;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import com.altafjava.platform.core.exception.ErrorCode;

/** Every school error code, registered with the platform's catalogue at {@code GET /api/v1/error-codes}. */
public final class SchoolErrorCodes {

	public static final List<ErrorCode> CATALOG = Stream.<ErrorCode[]>of(AdmissionErrorCode.values(),
			AcademicErrorCode.values(), AttendanceErrorCode.values(), FeeErrorCode.values(),
			CommunicationErrorCode.values(), StaffErrorCode.values(), StudentErrorCode.values(),
			LibraryErrorCode.values(), SchoolErrorCode.values())
			.flatMap(Arrays::stream)
			.toList();

	private SchoolErrorCodes() {
	}
}
