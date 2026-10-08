package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import java.time.DayOfWeek;
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
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

	@Mock
	private TimetableEntryRepository timetableEntryRepository;
	@Mock
	private PeriodRepository periodRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private TeacherRepository teacherRepository;

	private TimetableService timetableService;

	@BeforeEach
	void setUp() {
		timetableService = new TimetableService(timetableEntryRepository, periodRepository, classroomRepository,
				subjectRepository, teacherRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void stubAllReferencesExist() {
		when(periodRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(subjectRepository.existsByIdAndTenantId(3L, 1L)).thenReturn(true);
		when(teacherRepository.existsByIdAndTenantIdAndStatus(4L, 1L, EmployeeStatus.ACTIVE)).thenReturn(true);
	}

	@Test
	void schedule_withNonExistentPeriod_throwsResourceNotFound() {
		when(periodRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> timetableService.schedule(DayOfWeek.MONDAY, 1L, 2L, 3L, 4L));

		org.mockito.Mockito.verify(timetableEntryRepository, never()).save(any());
	}

	@Test
	void schedule_classroomAlreadyBookedForPeriod_throwsBusinessException() {
		stubAllReferencesExist();
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(1L, DayOfWeek.MONDAY, 1L,
				2L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> timetableService.schedule(DayOfWeek.MONDAY, 1L, 2L, 3L, 4L));

		org.mockito.Mockito.verify(timetableEntryRepository, never()).save(any());
	}

	@Test
	void schedule_teacherAlreadyBookedForPeriod_throwsBusinessException() {
		stubAllReferencesExist();
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(1L, DayOfWeek.MONDAY, 1L,
				2L)).thenReturn(false);
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(1L, DayOfWeek.MONDAY, 1L,
				4L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> timetableService.schedule(DayOfWeek.MONDAY, 1L, 2L, 3L, 4L));

		org.mockito.Mockito.verify(timetableEntryRepository, never()).save(any());
	}

	@Test
	void schedule_withNoConflicts_succeeds() {
		stubAllReferencesExist();
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(1L, DayOfWeek.MONDAY, 1L,
				2L)).thenReturn(false);
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(1L, DayOfWeek.MONDAY, 1L,
				4L)).thenReturn(false);
		when(timetableEntryRepository.save(any(TimetableEntry.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> timetableService.schedule(DayOfWeek.MONDAY, 1L, 2L, 3L, 4L));
	}

	@Test
	void schedule_sameTeacherDifferentClassroomsDifferentPeriods_succeeds() {
		// Same teacher, same day, but a *different* period — no conflict.
		when(periodRepository.existsByIdAndTenantId(5L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(subjectRepository.existsByIdAndTenantId(3L, 1L)).thenReturn(true);
		when(teacherRepository.existsByIdAndTenantIdAndStatus(4L, 1L, EmployeeStatus.ACTIVE)).thenReturn(true);
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(1L, DayOfWeek.MONDAY, 5L,
				2L)).thenReturn(false);
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(1L, DayOfWeek.MONDAY, 5L,
				4L)).thenReturn(false);
		when(timetableEntryRepository.save(any(TimetableEntry.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> timetableService.schedule(DayOfWeek.MONDAY, 5L, 2L, 3L, 4L));
	}
}
