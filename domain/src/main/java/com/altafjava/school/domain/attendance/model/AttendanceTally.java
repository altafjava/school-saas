package com.altafjava.school.domain.attendance.model;

/** How many days one student holds one status. */
public record AttendanceTally(Long studentId, AttendanceStatus status, long days) {
}
