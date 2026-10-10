package com.altafjava.school.application.service;

import static com.altafjava.school.application.support.TestEntities.activeTeacher;
import static com.altafjava.school.application.support.TestEntities.publicId;
import static com.altafjava.school.application.support.TestEntities.withId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.model.Venue;
import com.altafjava.school.domain.timetable.model.VenueType;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.domain.timetable.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

	private static final UUID VENUE_PUBLIC_ID = UUID.randomUUID();

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
	@Mock
	private VenueRepository venueRepository;
	@Mock
	private PublicIdLookup publicIdLookup;

	private TimetableService timetableService;

	@BeforeEach
	void setUp() {
		timetableService = new TimetableService(timetableEntryRepository, periodRepository, classroomRepository,
				subjectRepository, teacherRepository, venueRepository, publicIdLookup);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void stubAllReferencesExist() {
		when(periodRepository.findByPublicIdAndTenantId(publicId("period", 1), 1L)).thenReturn(Optional.of(withId(Period.class, 1L)));
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 2), 1L)).thenReturn(Optional.of(withId(Classroom.class, 2L)));
		when(subjectRepository.findByPublicIdAndTenantId(publicId("subject", 3), 1L)).thenReturn(Optional.of(withId(Subject.class, 3L)));
		when(teacherRepository.findByPublicIdAndTenantId(publicId("teacher", 4), 1L)).thenReturn(Optional.of(activeTeacher(4L)));
	}

	@Test
	void schedule_withNonExistentPeriod_throwsResourceNotFound() {
		when(periodRepository.findByPublicIdAndTenantId(publicId("period", 1), 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 1).toString(), publicId("classroom", 2).toString(), publicId("subject", 3).toString(), publicId("teacher", 4).toString(), null));

		org.mockito.Mockito.verify(timetableEntryRepository, never()).save(any());
	}

	@Test
	void schedule_classroomAlreadyBookedForPeriod_throwsBusinessException() {
		stubAllReferencesExist();
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(1L, DayOfWeek.MONDAY, 1L,
				2L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 1).toString(),
						publicId("classroom", 2).toString(), publicId("subject", 3).toString(),
						publicId("teacher", 4).toString(), null));

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
				() -> timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 1).toString(),
						publicId("classroom", 2).toString(), publicId("subject", 3).toString(),
						publicId("teacher", 4).toString(), null));

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

		assertDoesNotThrow(() -> timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 1).toString(),
				publicId("classroom", 2).toString(), publicId("subject", 3).toString(),
				publicId("teacher", 4).toString(), null));
	}

	@Test
	void schedule_sameTeacherDifferentClassroomsDifferentPeriods_succeeds() {
		// Same teacher, same day, but a *different* period — no conflict.
		when(periodRepository.findByPublicIdAndTenantId(publicId("period", 5), 1L)).thenReturn(Optional.of(withId(Period.class, 5L)));
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 2), 1L)).thenReturn(Optional.of(withId(Classroom.class, 2L)));
		when(subjectRepository.findByPublicIdAndTenantId(publicId("subject", 3), 1L)).thenReturn(Optional.of(withId(Subject.class, 3L)));
		when(teacherRepository.findByPublicIdAndTenantId(publicId("teacher", 4), 1L)).thenReturn(Optional.of(activeTeacher(4L)));
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(1L, DayOfWeek.MONDAY, 5L,
				2L)).thenReturn(false);
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(1L, DayOfWeek.MONDAY, 5L,
				4L)).thenReturn(false);
		when(timetableEntryRepository.save(any(TimetableEntry.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 5).toString(), publicId("classroom", 2).toString(), publicId("subject", 3).toString(), publicId("teacher", 4).toString(), null));
	}

	private Venue venueWithId(long id, boolean active) {
		Venue venue = Venue.create("LAB-1", "Physics Lab", VenueType.LABORATORY, 30);
		venue.setId(id);
		venue.setPublicId(VENUE_PUBLIC_ID);
		if (!active) {
			venue.deactivate();
		}
		return venue;
	}

	private TimetableEntry slotHoldingVenue(long entryId, long venueId) {
		TimetableEntry slot = TimetableEntry.create(DayOfWeek.MONDAY, 1L, 9L, 3L, 8L, venueId);
		slot.setId(entryId);
		return slot;
	}

	private void stubNoClassOrTeacherConflict() {
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(1L, DayOfWeek.MONDAY, 1L,
				2L)).thenReturn(false);
		when(timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(1L, DayOfWeek.MONDAY, 1L,
				4L)).thenReturn(false);
	}

	@Test
	void schedule_withAFreeVenue_holdsTheVenue() {
		stubAllReferencesExist();
		stubNoClassOrTeacherConflict();
		when(venueRepository.findByPublicIdAndTenantId(VENUE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(venueWithId(7L, true)));
		when(timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(1L, DayOfWeek.MONDAY, 1L))
				.thenReturn(List.of());
		when(timetableEntryRepository.save(any(TimetableEntry.class))).thenAnswer(inv -> inv.getArgument(0));

		TimetableEntry entry = timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 1).toString(),
				publicId("classroom", 2).toString(), publicId("subject", 3).toString(),
				publicId("teacher", 4).toString(),
				VENUE_PUBLIC_ID.toString());

		assertEquals(7L, entry.getVenueId());
	}

	@Test
	void schedule_withAVenueAlreadyBookedThatPeriod_throwsBusinessException() {
		stubAllReferencesExist();
		stubNoClassOrTeacherConflict();
		when(venueRepository.findByPublicIdAndTenantId(VENUE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(venueWithId(7L, true)));
		when(timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(1L, DayOfWeek.MONDAY, 1L))
				.thenReturn(List.of(slotHoldingVenue(50L, 7L)));

		assertThrows(BusinessException.class,
				() -> timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 1).toString(),
						publicId("classroom", 2).toString(), publicId("subject", 3).toString(),
						publicId("teacher", 4).toString(),
						VENUE_PUBLIC_ID.toString()));

		org.mockito.Mockito.verify(timetableEntryRepository, never()).save(any());
	}

	@Test
	void schedule_withADeactivatedVenue_throwsBusinessException() {
		stubAllReferencesExist();
		stubNoClassOrTeacherConflict();
		when(venueRepository.findByPublicIdAndTenantId(VENUE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(venueWithId(7L, false)));

		assertThrows(BusinessException.class,
				() -> timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 1).toString(),
						publicId("classroom", 2).toString(), publicId("subject", 3).toString(),
						publicId("teacher", 4).toString(),
						VENUE_PUBLIC_ID.toString()));
	}

	@Test
	void schedule_withAnUnknownVenue_throwsResourceNotFound() {
		stubAllReferencesExist();
		stubNoClassOrTeacherConflict();
		when(venueRepository.findByPublicIdAndTenantId(VENUE_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> timetableService.schedule(DayOfWeek.MONDAY, publicId("period", 1).toString(),
						publicId("classroom", 2).toString(), publicId("subject", 3).toString(),
						publicId("teacher", 4).toString(), VENUE_PUBLIC_ID.toString()));
	}

	@Test
	void assignVenue_toTheVenueTheSlotAlreadyHolds_isNotAConflictWithItself() {
		UUID entryPublicId = UUID.randomUUID();
		TimetableEntry entry = slotHoldingVenue(50L, 7L);
		when(timetableEntryRepository.findByPublicIdAndTenantId(entryPublicId, 1L)).thenReturn(Optional.of(entry));
		when(venueRepository.findByPublicIdAndTenantId(VENUE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(venueWithId(7L, true)));
		when(timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(1L, DayOfWeek.MONDAY, 1L))
				.thenReturn(List.of(entry));
		when(timetableEntryRepository.save(any(TimetableEntry.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> timetableService.assignVenue(entryPublicId.toString(), VENUE_PUBLIC_ID.toString(),
				ExpectedVersion.any()));
	}

	@Test
	void assignVenue_toAVenueAnotherSlotHoldsThatPeriod_throwsBusinessException() {
		UUID entryPublicId = UUID.randomUUID();
		TimetableEntry entry = slotHoldingVenue(50L, 6L);
		when(timetableEntryRepository.findByPublicIdAndTenantId(entryPublicId, 1L)).thenReturn(Optional.of(entry));
		when(venueRepository.findByPublicIdAndTenantId(VENUE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(venueWithId(7L, true)));
		when(timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(1L, DayOfWeek.MONDAY, 1L))
				.thenReturn(List.of(entry, slotHoldingVenue(51L, 7L)));

		assertThrows(BusinessException.class,
				() -> timetableService.assignVenue(entryPublicId.toString(), VENUE_PUBLIC_ID.toString(),
						ExpectedVersion.any()));
	}
}
