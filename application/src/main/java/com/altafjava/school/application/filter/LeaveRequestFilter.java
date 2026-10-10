package com.altafjava.school.application.filter;

import com.altafjava.school.domain.leave.model.LeaveRequestStatus;

/**
 * Narrows the leave-request lists; every part is optional. The dates select requests whose leave overlaps the
 * window, so a leave that starts before {@code from} but runs into it still matches.
 */
public record LeaveRequestFilter(String employeePublicId, String leaveTypePublicId, LeaveRequestStatus status,
		DateWindow dates) {

	public static final LeaveRequestFilter NONE = new LeaveRequestFilter(null, null, null, DateWindow.UNBOUNDED);
}
