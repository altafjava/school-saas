package com.altafjava.school.application.staffattendance;

import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.staffattendance.model.StaffAttendance;

/** One employee's line on the daily register; the attendance and the leave are absent when there is none. */
public record RegisterEntry(Employee employee, RegisterStatus status, StaffAttendance attendance,
		LeaveRequest leave) {
}
