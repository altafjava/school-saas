package com.altafjava.school.application.student;

/** Where a student sits in the current academic year. */
public record StudentPlacement(String classroomPublicId, String classroomName, String rollNumber,
		String academicYearPublicId) {
}
