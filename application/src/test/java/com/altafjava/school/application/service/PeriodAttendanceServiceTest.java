package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;
import com.altafjava.school.domain.attendance.repository.PeriodAttendanceRepository;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;

@ExtendWith(MockitoExtension.class)
class PeriodAttendanceServiceTest {

	@Mock
	private PeriodAttendanceRepository periodAttendanceRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private TimetableEntryRepository timetableEntryRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;

	private PeriodAttendanceService periodAttendanceService;

	@BeforeEach
	void setUp() {
		periodAttendanceService = new PeriodAttendanceService(periodAttendanceRepository, studentRepository,
				classroomRepository, timetableEntryRepository, studentClassroomLinkRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
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
		when(studentRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.mark(99L, 10L, 100L, LocalDate.now(), AttendanceStatus.PRESENT,
						"teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_withNonExistentClassroom_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.mark(1L, 99L, 100L, LocalDate.now(), AttendanceStatus.PRESENT,
						"teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_withNonExistentTimetableEntry_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(timetableEntryRepository.findByIdAndTenantId(100L, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.mark(1L, 10L, 100L, LocalDate.now(), AttendanceStatus.PRESENT,
						"teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_timetableEntryBelongsToDifferentClassroom_throwsBusinessException() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(timetableEntryRepository.findByIdAndTenantId(100L, 1L))
				.thenReturn(Optional.of(timetableEntryForClassroom(999L)));

		assertThrows(BusinessException.class,
				() -> periodAttendanceService.mark(1L, 10L, 100L, LocalDate.now(), AttendanceStatus.PRESENT,
						"teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_studentNotEnrolledInClassroom_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(timetableEntryRepository.findByIdAndTenantId(100L, 1L))
				.thenReturn(Optional.of(timetableEntryForClassroom(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.mark(1L, 10L, 100L, LocalDate.now(), AttendanceStatus.PRESENT,
						"teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_duplicateForSameStudentTimetableEntryDate_throwsIllegalArgument() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(timetableEntryRepository.findByIdAndTenantId(100L, 1L))
				.thenReturn(Optional.of(timetableEntryForClassroom(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		LocalDate date = LocalDate.now();
		when(periodAttendanceRepository.existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(1L, 100L,
				date, 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> periodAttendanceService.mark(1L, 10L, 100L, date, AttendanceStatus.PRESENT, "teacher"));

		verify(periodAttendanceRepository, never()).save(any());
	}

	@Test
	void mark_withValidReferences_succeeds() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(timetableEntryRepository.findByIdAndTenantId(100L, 1L))
				.thenReturn(Optional.of(timetableEntryForClassroom(10L)));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		when(periodAttendanceRepository.existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(any(),
				any(), any(), any())).thenReturn(false);
		when(periodAttendanceRepository.save(any(PeriodAttendance.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> periodAttendanceService.mark(1L, 10L, 100L, LocalDate.now(),
				AttendanceStatus.PRESENT, "teacher"));
	}
}
