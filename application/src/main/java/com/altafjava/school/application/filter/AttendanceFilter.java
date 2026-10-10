package com.altafjava.school.application.filter;

import com.altafjava.school.domain.attendance.model.AttendanceStatus;

/** Narrows the attendance list; every part is optional and composes with the caller's classroom scope. */
public record AttendanceFilter(String classroomPublicId, String studentPublicId, DateWindow dates,
		AttendanceStatus status) {

	public static final AttendanceFilter NONE = new AttendanceFilter(null, null, DateWindow.UNBOUNDED, null);
}
