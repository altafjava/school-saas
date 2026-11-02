package com.altafjava.school.application.filter;

import com.altafjava.school.domain.leave.model.LeaveRequestStatus;

/** Narrows the student-leave list; every part is optional. The dates match leave that overlaps the window. */
public record StudentLeaveRequestFilter(String studentPublicId, String classroomPublicId, LeaveRequestStatus status,
		DateWindow dates) {

	public static final StudentLeaveRequestFilter NONE = new StudentLeaveRequestFilter(null, null, null,
			DateWindow.UNBOUNDED);
}
