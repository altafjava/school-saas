package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TeachingAssignmentsTest {

	private static final Long HOMEROOM = 10L;
	private static final Long TIMETABLED = 20L;
	private static final Long OTHER = 30L;
	private static final Long MATH = 100L;
	private static final Long ENGLISH = 200L;

	private final TeachingAssignments teaching = new TeachingAssignments(5L, Set.of(HOMEROOM),
			Map.of(TIMETABLED, Set.of(MATH)));

	@Test
	void teachesClassroom_coversHomeroomAndTimetabledClassrooms() {
		assertTrue(teaching.teachesClassroom(HOMEROOM));
		assertTrue(teaching.teachesClassroom(TIMETABLED));
		assertFalse(teaching.teachesClassroom(OTHER));
	}

	@Test
	void teachesSubject_classTeacherCoversEverySubjectOfTheirClassroom() {
		assertTrue(teaching.teachesSubject(HOMEROOM, MATH));
		assertTrue(teaching.teachesSubject(HOMEROOM, ENGLISH));
	}

	@Test
	void teachesSubject_subjectTeacherCoversOnlyTheirTimetabledSubject() {
		assertTrue(teaching.teachesSubject(TIMETABLED, MATH));
		assertFalse(teaching.teachesSubject(TIMETABLED, ENGLISH));
		assertFalse(teaching.teachesSubject(OTHER, MATH));
	}

	@Test
	void classroomIds_isTheUnionOfBothKinds() {
		assertEquals(Set.of(HOMEROOM, TIMETABLED), teaching.classroomIds());
	}

	@Test
	void none_teachesNothing() {
		assertFalse(TeachingAssignments.NONE.isTeacher());
		assertFalse(TeachingAssignments.NONE.teachesClassroom(HOMEROOM));
		assertTrue(TeachingAssignments.NONE.classroomIds().isEmpty());
	}
}
