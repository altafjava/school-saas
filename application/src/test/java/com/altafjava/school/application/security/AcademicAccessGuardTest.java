package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableSubstitutionRepository;

@ExtendWith(MockitoExtension.class)
class AcademicAccessGuardTest {

	private static final Long TENANT_ID = 1L;
	private static final Long TEACHER_ID = 70L;
	private static final Long HOMEROOM = 10L;
	private static final Long TIMETABLED = 20L;
	private static final Long OTHER = 30L;
	private static final Long MATH = 100L;
	private static final Long ENGLISH = 200L;
	private static final Long OWN_STUDENT = 50L;
	private static final Long OTHER_STUDENT = 51L;
	private static final LocalDate DATE = LocalDate.of(2026, 10, 10);

	@Mock
	private AcademicScopeResolver academicScopeResolver;
	@Mock
	private PermissionAuthorizationService permissionAuthorizationService;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private TimetableSubstitutionRepository timetableSubstitutionRepository;
	@InjectMocks
	private AcademicAccessGuard guard;

	private void asTeacher() {
		when(academicScopeResolver.current(TENANT_ID)).thenReturn(new AcademicScope(false, false,
				new TeachingAssignments(TEACHER_ID, Set.of(HOMEROOM), Map.of(TIMETABLED, Set.of(MATH))), Set.of()));
	}

	private void asParent() {
		when(academicScopeResolver.current(TENANT_ID))
				.thenReturn(new AcademicScope(false, false, TeachingAssignments.NONE, Set.of(OWN_STUDENT)));
	}

	private void withScope(boolean readsAll, boolean writesAll) {
		when(academicScopeResolver.current(TENANT_ID))
				.thenReturn(new AcademicScope(readsAll, writesAll, TeachingAssignments.NONE, Set.of()));
	}

	@Test
	void writeClassroom_teacherOfClassroom_isAllowed() {
		asTeacher();

		assertDoesNotThrow(() -> guard.assertCanWriteClassroom(TENANT_ID, HOMEROOM));
		assertDoesNotThrow(() -> guard.assertCanWriteClassroom(TENANT_ID, TIMETABLED));
	}

	@Test
	void writeClassroom_teacherOfAnotherClassroom_isDenied() {
		asTeacher();

		assertThrows(AccessDeniedException.class, () -> guard.assertCanWriteClassroom(TENANT_ID, OTHER));
	}

	@Test
	void writeClassroom_readAllScope_isDenied() {
		withScope(true, false);

		assertThrows(AccessDeniedException.class, () -> guard.assertCanWriteClassroom(TENANT_ID, OTHER));
	}

	@Test
	void writeClassroom_writeAllScope_isAllowed() {
		withScope(true, true);

		assertDoesNotThrow(() -> guard.assertCanWriteClassroom(TENANT_ID, OTHER));
	}

	@Test
	void writeSubject_subjectTeacher_isLimitedToTheirSubject() {
		asTeacher();

		assertDoesNotThrow(() -> guard.assertCanWriteSubject(TENANT_ID, TIMETABLED, MATH));
		assertThrows(AccessDeniedException.class, () -> guard.assertCanWriteSubject(TENANT_ID, TIMETABLED, ENGLISH));
	}

	@Test
	void writeSubject_classTeacher_coversEverySubjectOfTheirClassroom() {
		asTeacher();

		assertDoesNotThrow(() -> guard.assertCanWriteSubject(TENANT_ID, HOMEROOM, ENGLISH));
	}

	@Test
	void readSubject_readAllScope_isAllowed() {
		withScope(true, false);

		assertDoesNotThrow(() -> guard.assertCanReadSubject(TENANT_ID, OTHER, MATH));
	}

	@Test
	void readSubject_parent_isDenied() {
		asParent();

		assertThrows(AccessDeniedException.class, () -> guard.assertCanReadSubject(TENANT_ID, HOMEROOM, MATH));
	}

	@Test
	void requireTeacherOfSubject_returnsTheCallersTeacherId() {
		asTeacher();

		assertEquals(TEACHER_ID, guard.requireTeacherOfSubject(TENANT_ID, TIMETABLED, MATH));
	}

	@Test
	void requireTeacherOfSubject_withoutTeacherRecord_isDeniedEvenWithWriteAll() {
		withScope(true, true);

		assertThrows(AccessDeniedException.class, () -> guard.requireTeacherOfSubject(TENANT_ID, HOMEROOM, MATH));
	}

	@Test
	void requireTeacherOfSubject_forSubjectNotTaught_isDenied() {
		asTeacher();

		assertThrows(AccessDeniedException.class,
				() -> guard.requireTeacherOfSubject(TENANT_ID, TIMETABLED, ENGLISH));
	}

	@Test
	void readStudentRecord_ownStudent_isAllowed() {
		asParent();

		assertDoesNotThrow(() -> guard.assertCanReadStudentRecord(TENANT_ID, HOMEROOM, OWN_STUDENT));
	}

	@Test
	void readStudentRecord_anotherStudent_isDenied() {
		asParent();

		assertThrows(AccessDeniedException.class,
				() -> guard.assertCanReadStudentRecord(TENANT_ID, HOMEROOM, OTHER_STUDENT));
	}

	@Test
	void readStudentRecord_staffWhoMayViewAnyStudent_isAllowed() {
		withScope(false, false);
		when(permissionAuthorizationService.hasPermission("STUDENT_READ")).thenReturn(true);

		assertDoesNotThrow(() -> guard.assertCanReadStudentRecord(TENANT_ID, OTHER, OTHER_STUDENT));
	}

	@Test
	void readRoster_enrolledStudentOrParent_isDenied() {
		asParent();

		assertThrows(AccessDeniedException.class, () -> guard.assertCanReadRoster(TENANT_ID, HOMEROOM));
	}

	@Test
	void readRoster_teacherOfClassroom_isAllowed() {
		asTeacher();

		assertDoesNotThrow(() -> guard.assertCanReadRoster(TENANT_ID, TIMETABLED));
	}

	@Test
	void viewCoursework_parentOfEnrolledStudent_isAllowed() {
		asParent();
		when(studentClassroomLinkRepository.existsByClassroomIdAndStudentIdIn(TENANT_ID, HOMEROOM,
				Set.of(OWN_STUDENT))).thenReturn(true);

		assertDoesNotThrow(() -> guard.assertCanViewCoursework(TENANT_ID, HOMEROOM));
	}

	@Test
	void viewCoursework_parentOfStudentInAnotherClassroom_isDenied() {
		asParent();

		assertThrows(AccessDeniedException.class, () -> guard.assertCanViewCoursework(TENANT_ID, OTHER));
	}

	@Test
	void viewCoursework_callerWithNoRecords_isDenied() {
		withScope(false, false);

		assertThrows(AccessDeniedException.class, () -> guard.assertCanViewCoursework(TENANT_ID, HOMEROOM));
	}

	@Test
	void markPeriod_timetabledTeacher_isAllowed() {
		asTeacher();

		assertDoesNotThrow(() -> guard.assertCanMarkPeriod(TENANT_ID, entry(TIMETABLED, MATH), DATE));
	}

	@Test
	void markPeriod_activeSubstitute_isAllowed() {
		asTeacher();
		TimetableEntry entry = entry(OTHER, ENGLISH);
		when(entry.getId()).thenReturn(900L);
		when(timetableSubstitutionRepository.existsActiveForSubstitute(TENANT_ID, 900L, DATE, TEACHER_ID))
				.thenReturn(true);

		assertDoesNotThrow(() -> guard.assertCanMarkPeriod(TENANT_ID, entry, DATE));
	}

	@Test
	void markPeriod_unrelatedTeacher_isDenied() {
		asTeacher();
		TimetableEntry entry = entry(OTHER, ENGLISH);

		assertThrows(AccessDeniedException.class, () -> guard.assertCanMarkPeriod(TENANT_ID, entry, DATE));
	}

	private TimetableEntry entry(Long classroomId, Long subjectId) {
		TimetableEntry entry = mock(TimetableEntry.class);
		when(entry.getClassroomId()).thenReturn(classroomId);
		when(entry.getSubjectId()).thenReturn(subjectId);
		return entry;
	}
}
