package com.altafjava.school.application.service;

import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.student.model.Student;

/** What a scan did: today's attendance for the cardholder, and whether it was recorded just now. */
public record AttendanceScanResult(Attendance attendance, Student student, Classroom classroom,
		boolean alreadyMarked) {
}
