package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Map;
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
import com.altafjava.school.application.filter.AttendanceFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.application.security.TeachingAssignments;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;
import com.altafjava.school.domain.attendance.repository.PeriodAttendanceRepository;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;

@ExtendWith(MockitoExtension.class)
class PeriodAttendanceServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID ENTRY_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private PeriodAttendanceRepository periodAttendanceRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TimetableEntryRepository timetableEntryRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private AcademicScopeResolver academicScopeResolver;
	@Mock
	private AcademicAccessGuard academicAccessGuard;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private PublicIdLookup publicIdLookup;

	private PeriodAttendanceService periodAttendanceService;

	@BeforeEach
	void setUp() {
		periodAttendanceService = new PeriodAttendanceService(periodAttendanceRepository, studentRepository,
				timetableEntryRepository, studentClassroomLinkRepository, academicScopeResolver,
				academicAccessGuard, studentDataAccessGuard, publicIdLookup);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		// An absent filter resolves to no id; a bare Mockito mock would answer 0L.
		lenient().when(publicIdLookup.idOrNull(any(), any())).thenReturn(null);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private TimetableEntry timetableEntryForClassroom(Long classroomId) {
		TimetableEntry entry = TimetableEntry.create(java.time.DayOfWeek.MONDAY, 3L, classroomId, 5L, 7L, null);
		entry.setId(100L);
		return entry;
	}

	@Test
	void mark_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.mark(STUDENT_PUBLIC_ID.toString(), ENTRY_PUBLIC_ID.toString(), LocalDate.now(), AttendanceStatus.PRESENT,
						"teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_withNonExistentTimetableEntry_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(timetableEntryRepository.findByPublicIdAndTenantId(ENTRY_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.mark(STUDENT_PUBLIC_ID.toString(), ENTRY_PUBLIC_ID.toString(), LocalDate.now(), AttendanceStatus.PRESENT,
						"teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_studentNotEnrolledInClassroom_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(timetableEntryRepository.findByPublicIdAndTenantId(ENTRY_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(timetableEntryForClassroom(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.mark(STUDENT_PUBLIC_ID.toString(), ENTRY_PUBLIC_ID.toString(), LocalDate.now(), AttendanceStatus.PRESENT,
						"teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_duplicateForSameStudentTimetableEntryDate_throwsIllegalArgument() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(timetableEntryRepository.findByPublicIdAndTenantId(ENTRY_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(timetableEntryForClassroom(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		LocalDate date = LocalDate.now();
		when(periodAttendanceRepository.existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(1L, 100L,
				date, 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> periodAttendanceService.mark(STUDENT_PUBLIC_ID.toString(), ENTRY_PUBLIC_ID.toString(), date, AttendanceStatus.PRESENT, "teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_withValidReferences_succeeds() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(timetableEntryRepository.findByPublicIdAndTenantId(ENTRY_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(timetableEntryForClassroom(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		when(periodAttendanceRepository.existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(any(),
				any(), any(), any())).thenReturn(false);
		when(periodAttendanceRepository.save(any(PeriodAttendance.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> periodAttendanceService.mark(STUDENT_PUBLIC_ID.toString(), ENTRY_PUBLIC_ID.toString(), LocalDate.now(),
				AttendanceStatus.PRESENT, "teacher"));
	}

	@Test
	void mark_byTeacherWhoDoesNotTeachThePeriod_throwsAccessDenied() {
		TimetableEntry entry = timetableEntryForClassroom(10L);
		LocalDate date = LocalDate.now();
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(1L)));
		when(timetableEntryRepository.findByPublicIdAndTenantId(ENTRY_PUBLIC_ID, 1L)).thenReturn(Optional.of(entry));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		doThrow(new AccessDeniedException("not scoped")).when(academicAccessGuard).assertCanMarkPeriod(1L, entry, date);

		assertThrows(AccessDeniedException.class,
				() -> periodAttendanceService.mark(STUDENT_PUBLIC_ID.toString(), ENTRY_PUBLIC_ID.toString(), date,
						AttendanceStatus.PRESENT, "teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void listAttendance_withAllClassroomsScope_searchesEveryClassroom() {
		when(academicScopeResolver.current(1L))
				.thenReturn(new AcademicScope(true, false, TeachingAssignments.NONE, Set.of()));
		PageRequest pageable = PageRequest.of(0, 20);
		when(periodAttendanceRepository.search(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any(),
				any())).thenReturn(Page.empty());

		periodAttendanceService.listAttendance(AttendanceFilter.NONE, pageable);

		verify(periodAttendanceRepository).search(1L, true, Set.of(), Set.of(), null, null, null, null, null,
				pageable);
	}

	@Test
	void listAttendance_asTeacher_scopesToTaughtClassroomsAndKeepsTheFilter() {
		TeachingAssignments teaching = new TeachingAssignments(70L, Set.of(), Map.of(10L, Set.of(5L)));
		when(academicScopeResolver.current(1L)).thenReturn(new AcademicScope(false, false, teaching, Set.of()));
		when(publicIdLookup.idOrNull(EntityRef.STUDENT, "student-public")).thenReturn(30L);
		PageRequest pageable = PageRequest.of(0, 20);
		LocalDate from = LocalDate.of(2026, 1, 1);
		when(periodAttendanceRepository.search(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any(),
				any())).thenReturn(Page.empty());

		periodAttendanceService.listAttendance(new AttendanceFilter(null, "student-public",
				new DateWindow(from, null), AttendanceStatus.ABSENT), pageable);

		verify(periodAttendanceRepository).search(1L, false, Set.of(10L), Set.of(), null, 30L, from, null,
				AttendanceStatus.ABSENT, pageable);
	}

	private Student studentWithId(Long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}
}
