package com.altafjava.school.application.home;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.model.TimetableSubstitution;
import com.altafjava.school.domain.timetable.model.Venue;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.domain.timetable.repository.TimetableSubstitutionRepository;
import com.altafjava.school.domain.timetable.repository.VenueRepository;
import lombok.RequiredArgsConstructor;

/** Today's slots for a teacher or a classroom. The regular timetable never changes; substitutions overlay it. */
@Component
@RequiredArgsConstructor
class TodayTimetableBuilder {

	private final TimetableEntryRepository timetableEntryRepository;
	private final TimetableSubstitutionRepository timetableSubstitutionRepository;
	private final PeriodRepository periodRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final TeacherRepository teacherRepository;
	private final VenueRepository venueRepository;

	private record Slot(TimetableEntry entry, SlotCoverage coverage, Long teacherId) {
	}

	private record Labels(Map<Long, Period> periods, Map<Long, Classroom> classrooms, Map<Long, Subject> subjects,
			Map<Long, Teacher> teachers, Map<Long, Venue> venues) {
	}

	List<TimetableSlot> forTeacher(Long tenantId, Long teacherId, HomeDay day) {
		if (!day.schoolDay() || !day.hasAcademicYear()) {
			return List.of();
		}
		List<TimetableEntry> own = timetableEntryRepository.findAllByTenantIdAndTeacherIdAndDayOfWeek(tenantId,
				teacherId, day.date().getDayOfWeek());
		List<TimetableSubstitution> covering = timetableSubstitutionRepository.findActiveCoveredBy(tenantId, day.date(),
				teacherId);
		Map<Long, TimetableSubstitution> coveredForMe = coverOf(tenantId, day, own);
		List<Slot> slots = new ArrayList<>(own.stream().map(entry -> slotOf(entry, coveredForMe)).toList());
		timetableEntryRepository.findAllByIdInAndTenantId(
				covering.stream().map(TimetableSubstitution::getTimetableEntryId).toList(), tenantId)
				.forEach(entry -> slots.add(new Slot(entry, SlotCoverage.COVERING, teacherId)));
		return toTimetable(tenantId, day, slots);
	}

	List<TimetableSlot> forClassroom(Long tenantId, Long classroomId, HomeDay day) {
		if (!day.schoolDay() || !day.hasAcademicYear()) {
			return List.of();
		}
		List<TimetableEntry> entries = timetableEntryRepository.findAllByTenantIdAndClassroomIdAndDayOfWeek(tenantId,
				classroomId, day.date().getDayOfWeek());
		Map<Long, TimetableSubstitution> cover = coverOf(tenantId, day, entries);
		return toTimetable(tenantId, day, entries.stream().map(entry -> slotOf(entry, cover)).toList());
	}

	private Map<Long, TimetableSubstitution> coverOf(Long tenantId, HomeDay day, List<TimetableEntry> entries) {
		if (entries.isEmpty()) {
			return Map.of();
		}
		return timetableSubstitutionRepository
				.findActiveForEntries(tenantId, day.date(), entries.stream().map(TimetableEntry::getId).toList())
				.stream().collect(Collectors.toMap(TimetableSubstitution::getTimetableEntryId, Function.identity()));
	}

	private static Slot slotOf(TimetableEntry entry, Map<Long, TimetableSubstitution> cover) {
		TimetableSubstitution substitution = cover.get(entry.getId());
		return substitution == null ? new Slot(entry, SlotCoverage.REGULAR, entry.getTeacherId())
				: new Slot(entry, SlotCoverage.SUBSTITUTED, substitution.getSubstituteTeacherId());
	}

	private List<TimetableSlot> toTimetable(Long tenantId, HomeDay day, List<Slot> slots) {
		if (slots.isEmpty()) {
			return List.of();
		}
		Labels labels = labelsFor(tenantId, slots);
		return slots.stream()
				.filter(slot -> inCurrentYear(slot, labels, day))
				.filter(slot -> labels.periods().containsKey(slot.entry().getPeriodId()))
				.sorted(Comparator.comparingInt(slot -> labels.periods().get(slot.entry().getPeriodId())
						.getDisplayOrder()))
				.map(slot -> toSlot(slot, labels))
				.toList();
	}

	// A promoted class keeps last year's timetable rows; they must not appear in this year's day.
	private static boolean inCurrentYear(Slot slot, Labels labels, HomeDay day) {
		Classroom classroom = labels.classrooms().get(slot.entry().getClassroomId());
		return classroom != null && day.academicYearId().equals(classroom.getAcademicYearId());
	}

	private Labels labelsFor(Long tenantId, List<Slot> slots) {
		Set<Long> teacherIds = slots.stream().map(Slot::teacherId).collect(Collectors.toCollection(HashSet::new));
		List<Long> venueIds = slots.stream().map(slot -> slot.entry().getVenueId())
				.filter(Objects::nonNull).distinct().toList();
		return new Labels(
				byId(periodRepository.findAllByTenantIdOrderByDisplayOrderAsc(tenantId), Period::getId),
				byId(classroomRepository.findAllByIdInAndTenantId(ids(slots, TimetableEntry::getClassroomId), tenantId),
						Classroom::getId),
				byId(subjectRepository.findAllByIdInAndTenantId(ids(slots, TimetableEntry::getSubjectId), tenantId),
						Subject::getId),
				byId(teacherRepository.findAllByIdInAndTenantId(List.copyOf(teacherIds), tenantId), Employee::getId),
				venueIds.isEmpty() ? Map.of()
						: byId(venueRepository.findAllByIdInAndTenantId(venueIds, tenantId), Venue::getId));
	}

	private static List<Long> ids(List<Slot> slots, Function<TimetableEntry, Long> field) {
		return slots.stream().map(Slot::entry).map(field).distinct().toList();
	}

	private static <T, K> Map<K, T> byId(Collection<T> records, Function<? super T, K> id) {
		return records.stream().collect(Collectors.toMap(id, Function.identity(), (first, duplicate) -> first));
	}

	private static TimetableSlot toSlot(Slot slot, Labels labels) {
		TimetableEntry entry = slot.entry();
		Period period = labels.periods().get(entry.getPeriodId());
		Venue venue = entry.getVenueId() == null ? null : labels.venues().get(entry.getVenueId());
		return new TimetableSlot(period.getName(), period.getStartTime(), period.getEndTime(),
				HomeReferences.of(labels.classrooms().get(entry.getClassroomId())),
				Stream.ofNullable(labels.subjects().get(entry.getSubjectId())).map(HomeReferences::of).findFirst()
						.orElse(null),
				Stream.ofNullable(labels.teachers().get(slot.teacherId())).map(HomeReferences::of).findFirst()
						.orElse(null),
				venue == null ? null : venue.getName(), slot.coverage());
	}
}
