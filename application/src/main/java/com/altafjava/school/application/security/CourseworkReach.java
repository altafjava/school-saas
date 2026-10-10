package com.altafjava.school.application.security;

import java.util.Set;

/** The classrooms whose lessons and assignments a caller may see: all of them, or a specific set. */
public record CourseworkReach(boolean everyClassroom, Set<Long> classroomIds) {

	public static final CourseworkReach ALL = new CourseworkReach(true, Set.of());

	public CourseworkReach {
		classroomIds = Set.copyOf(classroomIds);
	}
}
