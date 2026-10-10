package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;

@ExtendWith(MockitoExtension.class)
class TeachingAssignmentResolverTest {

	private static final Long TENANT_ID = 1L;
	private static final Long USER_ID = 7L;
	private static final Long TEACHER_ID = 70L;

	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private TimetableEntryRepository timetableEntryRepository;
	@InjectMocks
	private TeachingAssignmentResolver resolver;

	@Test
	void forUser_withoutTeacherRecord_teachesNothing() {
		when(teacherRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());

		assertSame(TeachingAssignments.NONE, resolver.forUser(USER_ID, TENANT_ID));
	}

	@Test
	void forUser_withoutUserId_teachesNothing() {
		assertSame(TeachingAssignments.NONE, resolver.forUser(null, TENANT_ID));
	}

	@Test
	void forUser_combinesHomeroomAndTimetabledSubjects() {
		Teacher teacher = mock(Teacher.class);
		when(teacher.getId()).thenReturn(TEACHER_ID);
		when(teacherRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(teacher));
		Classroom homeroom = mock(Classroom.class);
		when(homeroom.getId()).thenReturn(10L);
		when(classroomRepository.findAllByClassTeacherIdAndTenantId(TEACHER_ID, TENANT_ID))
				.thenReturn(List.of(homeroom));
		List<TimetableEntry> entries = List.of(entry(20L, 100L), entry(20L, 200L), entry(30L, 100L),
				entry(20L, 100L));
		when(timetableEntryRepository.findAllByTenantIdAndTeacherId(TENANT_ID, TEACHER_ID)).thenReturn(entries);

		TeachingAssignments teaching = resolver.forUser(USER_ID, TENANT_ID);

		assertEquals(TEACHER_ID, teaching.teacherId());
		assertEquals(Set.of(10L), teaching.homeroomClassroomIds());
		assertEquals(Map.of(20L, Set.of(100L, 200L), 30L, Set.of(100L)), teaching.subjectIdsByClassroomId());
	}

	private TimetableEntry entry(Long classroomId, Long subjectId) {
		TimetableEntry entry = mock(TimetableEntry.class);
		when(entry.getClassroomId()).thenReturn(classroomId);
		when(entry.getSubjectId()).thenReturn(subjectId);
		return entry;
	}
}
