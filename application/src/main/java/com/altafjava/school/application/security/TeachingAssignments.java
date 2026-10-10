package com.altafjava.school.application.security;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What one teacher teaches: the classrooms they lead as class teacher, and the subjects they are
 * timetabled for in any classroom.
 */
public record TeachingAssignments(Long teacherId, Set<Long> homeroomClassroomIds,
		Map<Long, Set<Long>> subjectIdsByClassroomId) {

	public static final TeachingAssignments NONE = new TeachingAssignments(null, Set.of(), Map.of());

	public TeachingAssignments {
		homeroomClassroomIds = Set.copyOf(homeroomClassroomIds);
		subjectIdsByClassroomId = Map.copyOf(subjectIdsByClassroomId);
	}

	public boolean isTeacher() {
		return teacherId != null;
	}

	public boolean teachesClassroom(Long classroomId) {
		return homeroomClassroomIds.contains(classroomId) || subjectIdsByClassroomId.containsKey(classroomId);
	}

	// A class teacher answers for every subject of their own classroom.
	public boolean teachesSubject(Long classroomId, Long subjectId) {
		return homeroomClassroomIds.contains(classroomId)
				|| subjectIdsByClassroomId.getOrDefault(classroomId, Set.of()).contains(subjectId);
	}

	public Set<Long> classroomIds() {
		Set<Long> ids = new HashSet<>(homeroomClassroomIds);
		ids.addAll(subjectIdsByClassroomId.keySet());
		return Set.copyOf(ids);
	}
}
