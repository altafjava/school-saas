package com.altafjava.school.application.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.model.TimetableSubstitution;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.domain.timetable.repository.TimetableSubstitutionRepository;
import com.altafjava.school.domain.timetable.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class TodayTimetableBuilderTest {

	private static final Long TENANT_ID = 1L;
	private static final Long TEACHER = 10L;
	private static final Long COLLEAGUE = 11L;
	private static final Long CLASSROOM = 20L;
	private static final Long LAST_YEARS_CLASSROOM = 21L;
	private static final Long YEAR = 5L;
	private static final Long PERIOD_ONE = 30L;
	private static final Long PERIOD_TWO = 31L;
	private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
	private static final HomeDay SCHOOL_DAY = new HomeDay(MONDAY.atTime(8, 0), true, YEAR, MONDAY, Set.of());

	@Mock
	private TimetableEntryRepository timetableEntryRepository;
	@Mock
	private TimetableSubstitutionRepository timetableSubstitutionRepository;
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
	@InjectMocks
	private TodayTimetableBuilder builder;

	@BeforeEach
	void labels() {
		Period first = period(PERIOD_ONE, "P1", 1);
		Period second = period(PERIOD_TWO, "P2", 2);
		lenient().when(periodRepository.findAllByTenantIdOrderByDisplayOrderAsc(TENANT_ID))
				.thenReturn(List.of(second, first));
		Classroom current = classroom(CLASSROOM, YEAR);
		Classroom previous = classroom(LAST_YEARS_CLASSROOM, YEAR - 1);
		lenient().when(classroomRepository.findAllByIdInAndTenantId(any(), eq(TENANT_ID)))
				.thenReturn(List.of(current, previous));
		Teacher me = teacher(TEACHER, "Mira");
		Teacher colleague = teacher(COLLEAGUE, "Cole");
		lenient().when(teacherRepository.findAllByIdInAndTenantId(any(), eq(TENANT_ID)))
				.thenReturn(List.of(me, colleague));
	}

	@Test
	void forTeacher_ordersSlotsByPeriod_andNamesTheTimetabledTeacher() {
		TimetableEntry second = entry(1L, PERIOD_TWO, CLASSROOM, TEACHER);
		TimetableEntry first = entry(2L, PERIOD_ONE, CLASSROOM, TEACHER);
		ownEntries(second, first);

		List<TimetableSlot> slots = builder.forTeacher(TENANT_ID, TEACHER, SCHOOL_DAY);

		assertEquals(List.of("P1", "P2"), slots.stream().map(TimetableSlot::periodName).toList());
		assertEquals(SlotCoverage.REGULAR, slots.getFirst().coverage());
		assertEquals("Mira Doe", slots.getFirst().teacher().name());
	}

	@Test
	void forTeacher_aSlotSomeoneElseCovers_namesTheSubstitute() {
		ownEntries(entry(1L, PERIOD_ONE, CLASSROOM, TEACHER));
		substitution(1L, COLLEAGUE);

		TimetableSlot slot = builder.forTeacher(TENANT_ID, TEACHER, SCHOOL_DAY).getFirst();

		assertEquals(SlotCoverage.SUBSTITUTED, slot.coverage());
		assertEquals("Cole Doe", slot.teacher().name());
	}

	@Test
	void forTeacher_aSlotTheyCover_joinsTheirDay() {
		ownEntries();
		TimetableEntry colleagues = entry(7L, PERIOD_TWO, CLASSROOM, COLLEAGUE);
		TimetableSubstitution cover = mock(TimetableSubstitution.class);
		when(cover.getTimetableEntryId()).thenReturn(7L);
		when(timetableSubstitutionRepository.findActiveCoveredBy(TENANT_ID, MONDAY, TEACHER))
				.thenReturn(List.of(cover));
		when(timetableEntryRepository.findAllByIdInAndTenantId(List.of(7L), TENANT_ID))
				.thenReturn(List.of(colleagues));

		TimetableSlot slot = builder.forTeacher(TENANT_ID, TEACHER, SCHOOL_DAY).getFirst();

		assertEquals(SlotCoverage.COVERING, slot.coverage());
		assertEquals("Mira Doe", slot.teacher().name());
	}

	@Test
	void forTeacher_leavesOutLastYearsClasses() {
		ownEntries(entry(1L, PERIOD_ONE, LAST_YEARS_CLASSROOM, TEACHER));

		assertTrue(builder.forTeacher(TENANT_ID, TEACHER, SCHOOL_DAY).isEmpty());
	}

	@Test
	void forTeacher_onADayOff_isEmptyWithoutAQuery() {
		HomeDay dayOff = new HomeDay(MONDAY.atTime(8, 0), false, YEAR, MONDAY, Set.of(MONDAY));

		assertTrue(builder.forTeacher(TENANT_ID, TEACHER, dayOff).isEmpty());
		verify(timetableEntryRepository, never()).findAllByTenantIdAndTeacherIdAndDayOfWeek(anyLong(), anyLong(),
				any());
	}

	@Test
	void forClassroom_aCoveredSlot_namesTheSubstitute() {
		TimetableEntry slot = entry(1L, PERIOD_ONE, CLASSROOM, TEACHER);
		when(timetableEntryRepository.findAllByTenantIdAndClassroomIdAndDayOfWeek(TENANT_ID, CLASSROOM,
				DayOfWeek.MONDAY)).thenReturn(List.of(slot));
		substitution(1L, COLLEAGUE);

		TimetableSlot result = builder.forClassroom(TENANT_ID, CLASSROOM, SCHOOL_DAY).getFirst();

		assertEquals(SlotCoverage.SUBSTITUTED, result.coverage());
		assertEquals("Cole Doe", result.teacher().name());
	}

	private void ownEntries(TimetableEntry... entries) {
		when(timetableEntryRepository.findAllByTenantIdAndTeacherIdAndDayOfWeek(TENANT_ID, TEACHER, DayOfWeek.MONDAY))
				.thenReturn(List.of(entries));
	}

	private void substitution(Long entryId, Long substituteId) {
		TimetableSubstitution substitution = mock(TimetableSubstitution.class);
		when(substitution.getTimetableEntryId()).thenReturn(entryId);
		when(substitution.getSubstituteTeacherId()).thenReturn(substituteId);
		when(timetableSubstitutionRepository.findActiveForEntries(eq(TENANT_ID), eq(MONDAY), any()))
				.thenReturn(List.of(substitution));
	}

	private static TimetableEntry entry(Long id, Long periodId, Long classroomId, Long teacherId) {
		TimetableEntry entry = mock(TimetableEntry.class);
		lenient().when(entry.getId()).thenReturn(id);
		lenient().when(entry.getPeriodId()).thenReturn(periodId);
		lenient().when(entry.getClassroomId()).thenReturn(classroomId);
		lenient().when(entry.getSubjectId()).thenReturn(40L);
		lenient().when(entry.getTeacherId()).thenReturn(teacherId);
		return entry;
	}

	private static Period period(Long id, String name, int order) {
		Period period = mock(Period.class);
		lenient().when(period.getId()).thenReturn(id);
		lenient().when(period.getName()).thenReturn(name);
		lenient().when(period.getDisplayOrder()).thenReturn(order);
		lenient().when(period.getStartTime()).thenReturn(LocalTime.of(8 + order, 0));
		lenient().when(period.getEndTime()).thenReturn(LocalTime.of(8 + order, 45));
		return period;
	}

	private static Classroom classroom(Long id, Long academicYearId) {
		Classroom classroom = mock(Classroom.class);
		lenient().when(classroom.getId()).thenReturn(id);
		lenient().when(classroom.getPublicId()).thenReturn(UUID.randomUUID());
		lenient().when(classroom.getAcademicYearId()).thenReturn(academicYearId);
		lenient().when(classroom.displayLabel()).thenReturn("5 A");
		lenient().when(classroom.getClassCode()).thenReturn("5A");
		return classroom;
	}

	private static Teacher teacher(Long id, String firstName) {
		Teacher teacher = mock(Teacher.class);
		lenient().when(teacher.getId()).thenReturn(id);
		lenient().when(teacher.getPublicId()).thenReturn(UUID.randomUUID());
		lenient().when(teacher.getFirstName()).thenReturn(firstName);
		lenient().when(teacher.getLastName()).thenReturn("Doe");
		lenient().when(teacher.getEmployeeCode()).thenReturn("EMP-" + id);
		return teacher;
	}
}
