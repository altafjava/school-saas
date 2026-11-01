package com.altafjava.school.api.dto.response.dashboard;

public record HrDashboardResponse(long teacherCount, long employeeCount, long departmentCount,
		long pendingLeaveRequestCount) {
}
