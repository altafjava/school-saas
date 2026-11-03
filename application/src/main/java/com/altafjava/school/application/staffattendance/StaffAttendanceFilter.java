package com.altafjava.school.application.staffattendance;

import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.domain.staffattendance.model.StaffAttendanceStatus;

/** Narrows the staff attendance list; every part is optional. */
public record StaffAttendanceFilter(String employeePublicId, StaffAttendanceStatus status, DateWindow dates) {
}
