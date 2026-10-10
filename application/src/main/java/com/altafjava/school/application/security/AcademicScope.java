package com.altafjava.school.application.security;

import java.util.Set;

/**
 * Where the caller's permissions apply: every classroom, the classrooms and subjects they teach,
 * or only their own students. A permission says what they may do; this says where.
 */
public record AcademicScope(boolean readsAllClassrooms, boolean writesAllClassrooms, TeachingAssignments teaching,
		Set<Long> ownStudentIds) {

	public static final AcademicScope NONE = new AcademicScope(false, false, TeachingAssignments.NONE, Set.of());

	public AcademicScope {
		ownStudentIds = Set.copyOf(ownStudentIds);
	}

	public boolean canReadClassroom(Long classroomId) {
		return readsAllClassrooms || teaching.teachesClassroom(classroomId);
	}

	public boolean canWriteClassroom(Long classroomId) {
		return writesAllClassrooms || teaching.teachesClassroom(classroomId);
	}

	public boolean canReadSubject(Long classroomId, Long subjectId) {
		return readsAllClassrooms || teaching.teachesSubject(classroomId, subjectId);
	}

	public boolean canWriteSubject(Long classroomId, Long subjectId) {
		return writesAllClassrooms || teaching.teachesSubject(classroomId, subjectId);
	}

	public boolean ownsStudent(Long studentId) {
		return ownStudentIds.contains(studentId);
	}
}
