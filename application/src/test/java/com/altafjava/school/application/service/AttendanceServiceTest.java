package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.application.security.TeachingAssignments;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceCorrection;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceCorrectionRepository;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID CLASSROOM_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private AttendanceCorrectionRepository attendanceCorrectionRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private AcademicScopeResolver academicScopeResolver;
	@Mock
	private AcademicAccessGuard academicAccessGuard;
	@Mock
	private HolidayService holidayService;

	private AttendanceService attendanceService;

	@BeforeEach
	void setUp() {
		attendanceService = new AttendanceService(attendanceRepository, attendanceCorrectionRepository,
				studentRepository, classroomRepository, studentClassroomLinkRepository, studentDataAccessGuard,
				academicScopeResolver, academicAccessGuard, holidayService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void mark_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.mark(STUDENT_PUBLIC_ID.toString(), CLASSROOM_PUBLIC_ID.toString(), LocalDate.now(), AttendanceStatus.PRESENT, "teacher"));

		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void mark_withNonExistentClassroom_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.mark(STUDENT_PUBLIC_ID.toString(), CLASSROOM_PUBLIC_ID.toString(), LocalDate.now(), AttendanceStatus.PRESENT, "teacher"));

		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void mark_studentNotEnrolledInClassroom_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L)).thenReturn(Optional.of(classroomWithId(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.mark(STUDENT_PUBLIC_ID.toString(), CLASSROOM_PUBLIC_ID.toString(), LocalDate.now(), AttendanceStatus.PRESENT, "teacher"));

		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void mark_duplicateForSameStudentClassroomDate_throwsIllegalArgument() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L)).thenReturn(Optional.of(classroomWithId(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		LocalDate date = LocalDate.now();
		when(attendanceRepository.existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(1L, 10L, date, 1L))
				.thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> attendanceService.mark(STUDENT_PUBLIC_ID.toString(), CLASSROOM_PUBLIC_ID.toString(), date, AttendanceStatus.PRESENT, "teacher"));
	}

	@Test
	void mark_withValidReferences_succeeds() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L)).thenReturn(Optional.of(classroomWithId(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		when(attendanceRepository.existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(
				any(), any(), any(), any())).thenReturn(false);
		when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> attendanceService.mark(STUDENT_PUBLIC_ID.toString(), CLASSROOM_PUBLIC_ID.toString(), LocalDate.now(), AttendanceStatus.PRESENT,
				"teacher"));
	}

	@Test
	void listAttendance_withAllClassroomsScope_returnsAllTenantAttendance() {
		when(academicScopeResolver.current(1L))
				.thenReturn(new AcademicScope(true, false, TeachingAssignments.NONE, Set.of()));
		PageRequest pageable = PageRequest.of(0, 20);
		when(attendanceRepository.findAllByTenantId(1L, pageable)).thenReturn(Page.empty());

		attendanceService.listAttendance(pageable);

		verify(attendanceRepository).findAllByTenantId(1L, pageable);
		verify(attendanceRepository, never()).findVisible(any(), any(), any(), any());
	}

	@Test
	void listAttendance_asTeacher_filtersByHomeroomAndTimetabledClassrooms() {
		TeachingAssignments teaching = new TeachingAssignments(70L, Set.of(10L), Map.of(11L, Set.of(5L)));
		when(academicScopeResolver.current(1L)).thenReturn(new AcademicScope(false, false, teaching, Set.of()));
		PageRequest pageable = PageRequest.of(0, 20);
		when(attendanceRepository.findVisible(1L, Set.of(10L, 11L), Set.of(), pageable)).thenReturn(Page.empty());

		attendanceService.listAttendance(pageable);

		verify(attendanceRepository).findVisible(1L, Set.of(10L, 11L), Set.of(), pageable);
		verify(attendanceRepository, never()).findAllByTenantId(any(), any());
	}

	@Test
	void listAttendance_asParent_filtersByOwnStudents() {
		when(academicScopeResolver.current(1L))
				.thenReturn(new AcademicScope(false, false, TeachingAssignments.NONE, Set.of(30L)));
		PageRequest pageable = PageRequest.of(0, 20);
		when(attendanceRepository.findVisible(1L, Set.of(), Set.of(30L), pageable)).thenReturn(Page.empty());

		attendanceService.listAttendance(pageable);

		verify(attendanceRepository).findVisible(1L, Set.of(), Set.of(30L), pageable);
	}

	@Test
	void findByPublicId_checksTheRecordIsWithinCallersScope() {
		String publicId = "11111111-1111-1111-1111-111111111111";
		Attendance attendance = Attendance.create(1L, 10L, LocalDate.now(), AttendanceStatus.ABSENT, "teacher");
		when(attendanceRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), 1L))
				.thenReturn(Optional.of(attendance));
		doThrow(new AccessDeniedException("not scoped")).when(academicAccessGuard).assertCanReadStudentRecord(1L, 10L,
				1L);

		assertThrows(AccessDeniedException.class, () -> attendanceService.findByPublicId(publicId));
	}

	@Test
	void mark_forClassroomOutsideCallersScope_throwsAccessDenied() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L)).thenReturn(Optional.of(classroomWithId(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		doThrow(new AccessDeniedException("not scoped")).when(academicAccessGuard).assertCanWriteClassroom(1L, 10L);

		assertThrows(AccessDeniedException.class,
				() -> attendanceService.mark(STUDENT_PUBLIC_ID.toString(), CLASSROOM_PUBLIC_ID.toString(), LocalDate.now(), AttendanceStatus.PRESENT, "teacher"));

		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void updateStatus_forClassroomOutsideCallersScope_throwsAccessDenied() {
		String publicId = "11111111-1111-1111-1111-111111111111";
		Attendance attendance = Attendance.create(1L, 10L, LocalDate.now(), AttendanceStatus.ABSENT, "teacher");
		when(attendanceRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), 1L))
				.thenReturn(Optional.of(attendance));
		doThrow(new AccessDeniedException("not scoped")).when(academicAccessGuard).assertCanWriteClassroom(1L, 10L);

		assertThrows(AccessDeniedException.class,
				() -> attendanceService.updateStatus(publicId, AttendanceStatus.PRESENT));

		verify(attendanceRepository, never()).save(any());
		verify(attendanceCorrectionRepository, never()).save(any());
	}

	@Test
	void getStudentAttendance_delegatesToAccessGuard() {
		var student = com.altafjava.school.domain.student.model.Student.create(
				"STU-1", "Alice", "Smith", "alice@school.test", null);
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(java.util.Optional.of(student));
		org.mockito.Mockito.doNothing().when(studentDataAccessGuard).assertCanView(any(), any());

		assertDoesNotThrow(() -> attendanceService.getStudentAttendance(
				"11111111-1111-1111-1111-111111111111", org.springframework.data.domain.PageRequest.of(0, 20)));

		verify(studentDataAccessGuard).assertCanView(1L, "11111111-1111-1111-1111-111111111111");
	}

	@Test
	void calculatePercentage_delegatesToAccessGuardAndCalculator() {
		var student = com.altafjava.school.domain.student.model.Student.create(
				"STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(42L);
		String studentPublicId = "11111111-1111-1111-1111-111111111111";
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student));
		org.mockito.Mockito.doNothing().when(studentDataAccessGuard).assertCanView(any(), any());
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetween(42L, 1L, from, to))
				.thenReturn(20L);
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(42L, 1L, from, to,
				AttendanceStatus.PRESENT)).thenReturn(18L);

		var result = attendanceService.calculatePercentage(studentPublicId, from, to);

		verify(studentDataAccessGuard).assertCanView(1L, studentPublicId);
		org.junit.jupiter.api.Assertions.assertEquals(18L, result.presentDays());
		org.junit.jupiter.api.Assertions.assertEquals(20L, result.totalMarkedDays());
		org.junit.jupiter.api.Assertions.assertEquals(0,
				java.math.BigDecimal.valueOf(90.00).compareTo(result.percentage()));
	}

	@Test
	void calculatePercentage_withHolidaysInRange_excludesThemFromCounts() {
		var student = com.altafjava.school.domain.student.model.Student.create(
				"STU-2", "Bob", "Jones", "bob@school.test", null);
		student.setId(43L);
		String studentPublicId = "22222222-2222-2222-2222-222222222222";
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		java.util.Set<LocalDate> holidays = java.util.Set.of(LocalDate.of(2026, 1, 26));
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student));
		org.mockito.Mockito.doNothing().when(studentDataAccessGuard).assertCanView(any(), any());
		when(holidayService.datesInRange(1L, from, to)).thenReturn(holidays);
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenExcludingDates(43L, 1L, from, to,
				holidays)).thenReturn(19L);
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatusExcludingDates(43L, 1L,
				from, to, AttendanceStatus.PRESENT, holidays)).thenReturn(17L);

		var result = attendanceService.calculatePercentage(studentPublicId, from, to);

		org.junit.jupiter.api.Assertions.assertEquals(17L, result.presentDays());
		org.junit.jupiter.api.Assertions.assertEquals(19L, result.totalMarkedDays());
		verify(attendanceRepository, never()).countByStudentIdAndTenantIdAndAttendanceDateBetween(any(), any(), any(),
				any());
	}

	@Test
	void calculatePercentage_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.calculatePercentage("11111111-1111-1111-1111-111111111111",
						LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

		verify(studentDataAccessGuard, never()).assertCanView(any(), any());
	}

	@Test
	void updateStatus_statusActuallyChanges_recordsCorrection() {
		String publicId = "11111111-1111-1111-1111-111111111111";
		Attendance attendance = Attendance.create(1L, 10L, LocalDate.now(), AttendanceStatus.ABSENT, "teacher");
		attendance.setId(55L);
		when(attendanceRepository.findByPublicIdAndTenantId(java.util.UUID.fromString(publicId), 1L))
				.thenReturn(Optional.of(attendance));
		when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

		attendanceService.updateStatus(publicId, AttendanceStatus.PRESENT);

		org.mockito.ArgumentCaptor<AttendanceCorrection> captor = org.mockito.ArgumentCaptor
				.forClass(AttendanceCorrection.class);
		verify(attendanceCorrectionRepository).save(captor.capture());
		org.junit.jupiter.api.Assertions.assertEquals(55L, captor.getValue().getAttendanceId());
		org.junit.jupiter.api.Assertions.assertEquals(AttendanceStatus.ABSENT, captor.getValue().getOldStatus());
		org.junit.jupiter.api.Assertions.assertEquals(AttendanceStatus.PRESENT, captor.getValue().getNewStatus());
	}

	@Test
	void updateStatus_statusUnchanged_doesNotRecordCorrection() {
		String publicId = "11111111-1111-1111-1111-111111111111";
		Attendance attendance = Attendance.create(1L, 10L, LocalDate.now(), AttendanceStatus.PRESENT, "teacher");
		attendance.setId(55L);
		when(attendanceRepository.findByPublicIdAndTenantId(java.util.UUID.fromString(publicId), 1L))
				.thenReturn(Optional.of(attendance));
		when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

		attendanceService.updateStatus(publicId, AttendanceStatus.PRESENT);

		verify(attendanceCorrectionRepository, never()).save(any());
	}

	private Student studentWithId(Long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private Classroom classroomWithId(Long id) {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 1L, "2025-26", null);
		classroom.setId(id);
		return classroom;
	}
}
