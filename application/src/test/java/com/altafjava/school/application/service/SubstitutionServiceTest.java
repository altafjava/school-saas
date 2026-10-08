package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.model.TimetableSubstitution;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.domain.timetable.repository.TimetableSubstitutionRepository;

@ExtendWith(MockitoExtension.class)
class SubstitutionServiceTest {

	private static final Long TENANT_ID = 1L;
	private static final Long PERIOD_ID = 5L;
	private static final Long REGULAR_TEACHER_ID = 4L;
	private static final Long SUBSTITUTE_ID = 9L;
	private static final UUID ENTRY_PUBLIC_ID = UUID.randomUUID();
	private static final UUID SUBSTITUTE_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private TimetableSubstitutionRepository substitutionRepository;
	@Mock
	private TimetableEntryRepository timetableEntryRepository;
	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private LeaveRequestRepository leaveRequestRepository;
	@Mock
	private HolidayService holidayService;
	@Mock
	private NotificationService notificationService;

	private SubstitutionService service;
	private LocalDate nextMonday;
	private TimetableEntry entry;

	@BeforeEach
	void setUp() {
		service = new SubstitutionService(substitutionRepository, timetableEntryRepository, teacherRepository,
				leaveRequestRepository, holidayService, notificationService);
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
		LocalDate today = LocalDate.now();
		nextMonday = today.plusDays(((DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue()) + 7) % 7 + 7);
		entry = entryTaughtBy(50L, REGULAR_TEACHER_ID);
		lenient().when(timetableEntryRepository.findByPublicIdAndTenantId(ENTRY_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.of(entry));
		lenient().when(holidayService.datesInRange(TENANT_ID, nextMonday, nextMonday)).thenReturn(Set.of());
		lenient().when(substitutionRepository.findActiveOnDateAndPeriod(TENANT_ID, nextMonday, PERIOD_ID))
				.thenReturn(List.of());
		lenient().when(leaveRequestRepository.findEmployeeIdsOnApprovedLeaveOn(TENANT_ID, nextMonday))
				.thenReturn(List.of());
		lenient().when(timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(TENANT_ID,
				DayOfWeek.MONDAY, PERIOD_ID)).thenReturn(List.of(entry));
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private TimetableEntry entryTaughtBy(long id, long teacherId) {
		TimetableEntry slot = TimetableEntry.create(DayOfWeek.MONDAY, PERIOD_ID, 2L, 3L, teacherId, null);
		slot.setId(id);
		slot.setPublicId(id == 50L ? ENTRY_PUBLIC_ID : UUID.randomUUID());
		return slot;
	}

	private Teacher teacher(long id, UUID publicId, Long userId) {
		Teacher teacher = Teacher.create("T-" + id, "Teacher", "No" + id, "t" + id + "@school.test",
				LocalDate.of(2020, 1, 1));
		teacher.setId(id);
		teacher.setPublicId(publicId);
		teacher.setUserId(userId);
		return teacher;
	}

	private Teacher stubSubstituteTeacher(Long userId) {
		Teacher substitute = teacher(SUBSTITUTE_ID, SUBSTITUTE_PUBLIC_ID, userId);
		when(teacherRepository.findByPublicIdAndTenantId(SUBSTITUTE_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.of(substitute));
		return substitute;
	}

	private void stubSaveAndDetails(Teacher substitute) {
		when(substitutionRepository.save(any(TimetableSubstitution.class))).thenAnswer(inv -> inv.getArgument(0));
		when(timetableEntryRepository.findAllByIdInAndTenantId(List.of(50L), TENANT_ID)).thenReturn(List.of(entry));
		when(teacherRepository.findAllByIdInAndTenantId(any(), any()))
				.thenReturn(List.of(teacher(REGULAR_TEACHER_ID, UUID.randomUUID(), null), substitute));
	}

	@Test
	void assign_toAFreeTeacher_savesAndNotifiesThem() {
		Teacher substitute = stubSubstituteTeacher(300L);
		stubSaveAndDetails(substitute);

		SubstitutionDetails details = service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday,
				SUBSTITUTE_PUBLIC_ID.toString(), "Sick leave", 77L);

		assertEquals(SUBSTITUTE_ID, details.substitution().getSubstituteTeacherId());
		assertEquals(nextMonday, details.substitution().getSubstitutionDate());
		verify(notificationService).send(any(SendNotificationCommand.class));
	}

	@Test
	void assign_toATeacherWithNoLogin_savesWithoutNotifying() {
		Teacher substitute = stubSubstituteTeacher(null);
		stubSaveAndDetails(substitute);

		service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday, SUBSTITUTE_PUBLIC_ID.toString(), null, 77L);

		verify(notificationService, never()).send(any(SendNotificationCommand.class));
	}

	@Test
	void assign_toATeacherTeachingAnotherClassThatPeriod_throwsBusinessException() {
		stubSubstituteTeacher(null);
		when(timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(TENANT_ID, DayOfWeek.MONDAY,
				PERIOD_ID)).thenReturn(List.of(entry, entryTaughtBy(51L, SUBSTITUTE_ID)));

		assertThrows(BusinessException.class, () -> service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday,
				SUBSTITUTE_PUBLIC_ID.toString(), null, 77L));

		verify(substitutionRepository, never()).save(any());
	}

	@Test
	void assign_toATeacherWhoseOwnClassThatPeriodIsAlreadyCovered_isAllowed() {
		Teacher substitute = stubSubstituteTeacher(null);
		TimetableEntry substitutesOwnSlot = entryTaughtBy(51L, SUBSTITUTE_ID);
		TimetableSubstitution coverForTheirSlot = TimetableSubstitution.assign(substitutesOwnSlot, nextMonday, 12L,
				null, 1L, LocalDate.now());
		when(timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(TENANT_ID, DayOfWeek.MONDAY,
				PERIOD_ID)).thenReturn(List.of(entry, substitutesOwnSlot));
		when(substitutionRepository.findActiveOnDateAndPeriod(TENANT_ID, nextMonday, PERIOD_ID))
				.thenReturn(List.of(coverForTheirSlot));
		stubSaveAndDetails(substitute);

		SubstitutionDetails details = service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday,
				SUBSTITUTE_PUBLIC_ID.toString(), null, 77L);

		assertEquals(SUBSTITUTE_ID, details.substitution().getSubstituteTeacherId());
	}

	@Test
	void assign_toATeacherAlreadyCoveringAnotherClassThatPeriod_throwsBusinessException() {
		stubSubstituteTeacher(null);
		TimetableEntry otherSlot = entryTaughtBy(51L, 33L);
		TimetableSubstitution existingCover = TimetableSubstitution.assign(otherSlot, nextMonday, SUBSTITUTE_ID, null,
				1L, LocalDate.now());
		when(substitutionRepository.findActiveOnDateAndPeriod(TENANT_ID, nextMonday, PERIOD_ID))
				.thenReturn(List.of(existingCover));

		assertThrows(BusinessException.class, () -> service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday,
				SUBSTITUTE_PUBLIC_ID.toString(), null, 77L));
	}

	@Test
	void assign_toATeacherOnApprovedLeave_throwsBusinessException() {
		stubSubstituteTeacher(null);
		when(leaveRequestRepository.findEmployeeIdsOnApprovedLeaveOn(TENANT_ID, nextMonday))
				.thenReturn(List.of(SUBSTITUTE_ID));

		assertThrows(BusinessException.class, () -> service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday,
				SUBSTITUTE_PUBLIC_ID.toString(), null, 77L));
	}

	@Test
	void assign_whenTheSlotIsAlreadyCovered_throwsBusinessException() {
		stubSubstituteTeacher(null);
		when(substitutionRepository.existsActiveFor(TENANT_ID, 50L, nextMonday)).thenReturn(true);

		assertThrows(BusinessException.class, () -> service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday,
				SUBSTITUTE_PUBLIC_ID.toString(), null, 77L));
	}

	@Test
	void assign_onASchoolHoliday_throwsBusinessException() {
		stubSubstituteTeacher(null);
		when(holidayService.datesInRange(TENANT_ID, nextMonday, nextMonday)).thenReturn(Set.of(nextMonday));

		assertThrows(BusinessException.class, () -> service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday,
				SUBSTITUTE_PUBLIC_ID.toString(), null, 77L));
	}

	@Test
	void assign_toAnInactiveOrUnknownTeacher_throwsResourceNotFound() {
		when(teacherRepository.findByPublicIdAndTenantId(SUBSTITUTE_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.assign(ENTRY_PUBLIC_ID.toString(), nextMonday,
				SUBSTITUTE_PUBLIC_ID.toString(), null, 77L));
	}

	@Test
	void availableTeachers_excludeTheBusyTheOnLeaveAndTheRegularTeacher() {
		Teacher regular = teacher(REGULAR_TEACHER_ID, UUID.randomUUID(), null);
		Teacher teaching = teacher(11L, UUID.randomUUID(), null);
		Teacher onLeave = teacher(12L, UUID.randomUUID(), null);
		Teacher free = teacher(13L, UUID.randomUUID(), null);
		when(timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(TENANT_ID, DayOfWeek.MONDAY,
				PERIOD_ID)).thenReturn(List.of(entry, entryTaughtBy(51L, 11L)));
		when(leaveRequestRepository.findEmployeeIdsOnApprovedLeaveOn(TENANT_ID, nextMonday))
				.thenReturn(List.of(12L));
		when(teacherRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(regular, teaching, onLeave, free));

		List<Teacher> available = service.availableTeachers(ENTRY_PUBLIC_ID.toString(), nextMonday);

		assertEquals(List.of(free), available);
	}

	@Test
	void availableTeachers_forADateOnTheWrongWeekday_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> service.availableTeachers(ENTRY_PUBLIC_ID.toString(), nextMonday.plusDays(1)));
	}

	@Test
	void uncoveredOn_listsSlotsOfTeachersOnLeaveWithoutACover() {
		TimetableEntry coveredSlot = entryTaughtBy(51L, 12L);
		TimetableEntry teacherPresentSlot = entryTaughtBy(52L, 13L);
		TimetableSubstitution coverForSecond = TimetableSubstitution.assign(coveredSlot, nextMonday, 20L, null, 1L,
				LocalDate.now());
		when(leaveRequestRepository.findEmployeeIdsOnApprovedLeaveOn(TENANT_ID, nextMonday))
				.thenReturn(List.of(REGULAR_TEACHER_ID, 12L));
		when(substitutionRepository.findActiveOn(TENANT_ID, nextMonday)).thenReturn(List.of(coverForSecond));
		when(timetableEntryRepository.findAllByTenantIdAndDayOfWeek(TENANT_ID, DayOfWeek.MONDAY))
				.thenReturn(List.of(entry, coveredSlot, teacherPresentSlot));

		List<TimetableEntry> uncovered = service.uncoveredOn(nextMonday);

		assertEquals(List.of(entry), uncovered);
	}

	@Test
	void uncoveredOn_aHoliday_isEmpty() {
		when(holidayService.datesInRange(TENANT_ID, nextMonday, nextMonday)).thenReturn(Set.of(nextMonday));

		assertTrue(service.uncoveredOn(nextMonday).isEmpty());
	}

	@Test
	void cancel_marksTheSubstitutionCancelled() {
		TimetableSubstitution substitution = TimetableSubstitution.assign(entry, nextMonday, SUBSTITUTE_ID, null, 1L,
				LocalDate.now());
		UUID publicId = UUID.randomUUID();
		substitution.setPublicId(publicId);
		when(substitutionRepository.findByPublicIdAndTenantId(publicId, TENANT_ID))
				.thenReturn(Optional.of(substitution));
		stubSaveAndDetails(teacher(SUBSTITUTE_ID, SUBSTITUTE_PUBLIC_ID, null));

		service.cancel(publicId.toString(), "Teacher returned");

		assertEquals("Teacher returned", substitution.getCancellationReason());
	}
}
