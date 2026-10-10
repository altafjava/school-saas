package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.filter.CourseworkFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.CourseworkReach;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.lms.model.Lesson;
import com.altafjava.school.domain.lms.repository.LessonRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

	private static final UUID CLASSROOM_PUBLIC_ID = UUID.randomUUID();
	private static final UUID SUBJECT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private LessonRepository lessonRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private AcademicAccessGuard academicAccessGuard;
	@Mock
	private PublicIdLookup publicIdLookup;

	private LessonService lessonService;

	@BeforeEach
	void setUp() {
		lessonService = new LessonService(lessonRepository, classroomRepository, subjectRepository,
				academicAccessGuard, publicIdLookup);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		// An absent filter resolves to no id; a bare Mockito mock would answer 0L.
		lenient().when(publicIdLookup.idOrNull(any(), any())).thenReturn(null);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Classroom classroomWithId(long id) {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 10L, "2024-25", null);
		classroom.setId(id);
		return classroom;
	}

	private Subject subjectWithId(long id) {
		Subject subject = Subject.create("SUB-1", "Science", null);
		subject.setId(id);
		return subject;
	}

	@Test
	void post_withNonExistentClassroom_throwsResourceNotFound() {
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> lessonService.post(CLASSROOM_PUBLIC_ID.toString(),
				SUBJECT_PUBLIC_ID.toString(), "Title", "Desc", null));

		verify(lessonRepository, never()).save(any());
	}

	@Test
	void post_teacherNotScopedToClassroom_throwsAccessDenied() {
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(classroomWithId(5L)));
		when(subjectRepository.findByPublicIdAndTenantId(SUBJECT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(subjectWithId(6L)));
		when(academicAccessGuard.requireTeacherOfSubject(1L, 5L, 6L))
				.thenThrow(new AccessDeniedException("not scoped"));

		assertThrows(AccessDeniedException.class, () -> lessonService.post(CLASSROOM_PUBLIC_ID.toString(),
				SUBJECT_PUBLIC_ID.toString(), "Title", "Desc", null));

		verify(lessonRepository, never()).save(any());
	}

	@Test
	void post_withValidReferences_succeeds() {
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(classroomWithId(5L)));
		when(subjectRepository.findByPublicIdAndTenantId(SUBJECT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(subjectWithId(6L)));
		when(academicAccessGuard.requireTeacherOfSubject(1L, 5L, 6L)).thenReturn(7L);
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

		Lesson lesson = assertDoesNotThrow(() -> lessonService.post(CLASSROOM_PUBLIC_ID.toString(),
				SUBJECT_PUBLIC_ID.toString(), "Title", "Desc", "key"));

		assertEquals(5L, lesson.getClassroomId());
		assertEquals(6L, lesson.getSubjectId());
		assertEquals(7L, lesson.getTeacherId());
	}

	@Test
	void listLessons_forAClassroom_requiresItToBeReachableAndFiltersByPostingDay() {
		when(publicIdLookup.idOrNull(EntityRef.CLASSROOM, CLASSROOM_PUBLIC_ID.toString())).thenReturn(5L);
		when(academicAccessGuard.courseworkReach(1L, 5L)).thenReturn(CourseworkReach.ALL);
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		Page<Lesson> expected = Page.empty();
		when(lessonRepository.search(1L, true, Set.of(), 5L, null, from.atStartOfDay(),
				to.plusDays(1).atStartOfDay(), PageRequest.of(0, 20))).thenReturn(expected);

		Page<Lesson> result = lessonService.listLessons(
				new CourseworkFilter(CLASSROOM_PUBLIC_ID.toString(), null, new DateWindow(from, to)),
				PageRequest.of(0, 20));

		assertEquals(expected, result);
	}

	@Test
	void listLessons_forAnUnreachableClassroom_propagatesAccessDenied() {
		when(publicIdLookup.idOrNull(EntityRef.CLASSROOM, CLASSROOM_PUBLIC_ID.toString())).thenReturn(5L);
		when(academicAccessGuard.courseworkReach(1L, 5L)).thenThrow(new AccessDeniedException("denied"));

		assertThrows(AccessDeniedException.class, () -> lessonService.listLessons(
				new CourseworkFilter(CLASSROOM_PUBLIC_ID.toString(), null, DateWindow.UNBOUNDED),
				PageRequest.of(0, 20)));
	}

	@Test
	void listLessons_withoutAClassroom_searchesTheClassroomsTheCallerReaches() {
		when(academicAccessGuard.courseworkReach(1L, null)).thenReturn(new CourseworkReach(false, Set.of(5L)));
		when(lessonRepository.search(1L, false, Set.of(5L), null, null, null, null, PageRequest.of(0, 20)))
				.thenReturn(Page.empty());

		lessonService.listLessons(CourseworkFilter.NONE, PageRequest.of(0, 20));

		verify(lessonRepository).search(1L, false, Set.of(5L), null, null, null, null, PageRequest.of(0, 20));
	}
}
