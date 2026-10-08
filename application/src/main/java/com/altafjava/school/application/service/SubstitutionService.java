package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.model.TimetableSubstitution;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.domain.timetable.repository.TimetableSubstitutionRepository;
import lombok.RequiredArgsConstructor;

/**
 * Covers a regular timetable slot with another teacher on a given date. A teacher is free to cover
 * a slot only if nothing else claims them in that period that day: their own class (unless it is
 * itself covered), another cover, or approved leave.
 */
@Service
@RequiredArgsConstructor
public class SubstitutionService {

	private final TimetableSubstitutionRepository substitutionRepository;
	private final TimetableEntryRepository timetableEntryRepository;
	private final TeacherRepository teacherRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final HolidayService holidayService;
	private final NotificationService notificationService;

	@Transactional(readOnly = true)
	public List<SubstitutionDetails> listOn(LocalDate date) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return details(tenantId, substitutionRepository.findActiveOn(tenantId, date));
	}

	@Transactional(readOnly = true)
	public List<Teacher> availableTeachers(String entryPublicId, LocalDate date) {
		Long tenantId = TenantContext.getCurrentTenantId();
		TimetableEntry entry = requireEntry(tenantId, entryPublicId);
		requireSlotFallsOn(entry, date);
		Set<Long> busy = busyTeacherIds(tenantId, entry, date);
		return teacherRepository.findAllByTenantId(tenantId).stream()
				.filter(Teacher::isActive)
				.filter(teacher -> !busy.contains(teacher.getId()) && !teacher.getId().equals(entry.getTeacherId()))
				.toList();
	}

	// Slots whose regular teacher is on approved leave that day and still has nobody covering.
	@Transactional(readOnly = true)
	public List<TimetableEntry> uncoveredOn(LocalDate date) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!holidayService.datesInRange(tenantId, date, date).isEmpty()) {
			return List.of();
		}
		Set<Long> onLeave = Set.copyOf(leaveRequestRepository.findEmployeeIdsOnApprovedLeaveOn(tenantId, date));
		Set<Long> coveredEntryIds = substitutionRepository.findActiveOn(tenantId, date).stream()
				.map(TimetableSubstitution::getTimetableEntryId)
				.collect(Collectors.toSet());
		return timetableEntryRepository.findAllByTenantIdAndDayOfWeek(tenantId, date.getDayOfWeek()).stream()
				.filter(entry -> onLeave.contains(entry.getTeacherId()) && !coveredEntryIds.contains(entry.getId()))
				.toList();
	}

	@Transactional
	public SubstitutionDetails assign(String entryPublicId, LocalDate date, String substituteTeacherPublicId,
			String reason, Long assignedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		TimetableEntry entry = requireEntry(tenantId, entryPublicId);
		Teacher substitute = teacherRepository
				.findByPublicIdAndTenantId(UUID.fromString(substituteTeacherPublicId), tenantId)
				.filter(Teacher::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + substituteTeacherPublicId));
		TimetableSubstitution substitution = TimetableSubstitution.assign(entry, date, substitute.getId(), reason,
				assignedByUserId, LocalDate.now());
		requireWorkingDay(tenantId, date);
		if (substitutionRepository.existsActiveFor(tenantId, entry.getId(), date)) {
			throw new BusinessException("A substitute is already assigned to this slot on " + date);
		}
		if (busyTeacherIds(tenantId, entry, date).contains(substitute.getId())) {
			throw new BusinessException(
					substitute.getFirstName() + " " + substitute.getLastName() + " is not free to cover on " + date);
		}
		TimetableSubstitution saved = substitutionRepository.save(substitution);
		notifySubstitute(tenantId, substitute, date);
		return details(tenantId, List.of(saved)).get(0);
	}

	@Transactional
	public SubstitutionDetails cancel(String publicId, String reason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		TimetableSubstitution substitution = substitutionRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Substitution not found: " + publicId));
		substitution.cancel(reason, LocalDate.now());
		return details(tenantId, List.of(substitutionRepository.save(substitution))).get(0);
	}

	private Set<Long> busyTeacherIds(Long tenantId, TimetableEntry entry, LocalDate date) {
		List<TimetableSubstitution> covers = substitutionRepository.findActiveOnDateAndPeriod(tenantId, date,
				entry.getPeriodId());
		Set<Long> coveredEntryIds = covers.stream().map(TimetableSubstitution::getTimetableEntryId)
				.collect(Collectors.toSet());
		Set<Long> busy = new HashSet<>();
		timetableEntryRepository.findAllByTenantIdAndDayOfWeekAndPeriodId(tenantId, entry.getDayOfWeek(),
				entry.getPeriodId()).stream()
				.filter(slot -> !coveredEntryIds.contains(slot.getId()))
				.forEach(slot -> busy.add(slot.getTeacherId()));
		covers.forEach(cover -> busy.add(cover.getSubstituteTeacherId()));
		busy.addAll(leaveRequestRepository.findEmployeeIdsOnApprovedLeaveOn(tenantId, date));
		return busy;
	}

	private void requireSlotFallsOn(TimetableEntry entry, LocalDate date) {
		if (date.getDayOfWeek() != entry.getDayOfWeek()) {
			throw new BusinessException("The slot takes place on " + entry.getDayOfWeek() + ", but " + date
					+ " is a " + date.getDayOfWeek());
		}
	}

	private void requireWorkingDay(Long tenantId, LocalDate date) {
		if (!holidayService.datesInRange(tenantId, date, date).isEmpty()) {
			throw new BusinessException(date + " is a school holiday");
		}
	}

	private TimetableEntry requireEntry(Long tenantId, String entryPublicId) {
		return timetableEntryRepository.findByPublicIdAndTenantId(UUID.fromString(entryPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found: " + entryPublicId));
	}

	private List<SubstitutionDetails> details(Long tenantId, List<TimetableSubstitution> substitutions) {
		if (substitutions.isEmpty()) {
			return List.of();
		}
		Map<Long, TimetableEntry> entries = timetableEntryRepository
				.findAllByIdInAndTenantId(substitutions.stream().map(TimetableSubstitution::getTimetableEntryId)
						.distinct().toList(), tenantId)
				.stream().collect(Collectors.toMap(TimetableEntry::getId, Function.identity()));
		Set<Long> teacherIds = new HashSet<>();
		entries.values().forEach(entry -> teacherIds.add(entry.getTeacherId()));
		substitutions.forEach(substitution -> teacherIds.add(substitution.getSubstituteTeacherId()));
		Map<Long, Teacher> teachers = teacherRepository.findAllByIdInAndTenantId(List.copyOf(teacherIds), tenantId)
				.stream().collect(Collectors.toMap(Teacher::getId, Function.identity()));
		return substitutions.stream().map(substitution -> {
			TimetableEntry entry = entries.get(substitution.getTimetableEntryId());
			return new SubstitutionDetails(substitution, entry, teachers.get(entry.getTeacherId()),
					teachers.get(substitution.getSubstituteTeacherId()));
		}).toList();
	}

	private void notifySubstitute(Long tenantId, Teacher substitute, LocalDate date) {
		if (substitute.getUserId() == null) {
			return;
		}
		notificationService.send(SendNotificationCommand.builder()
				.tenantId(tenantId)
				.userId(substitute.getUserId())
				.type(NotificationType.ANNOUNCEMENT)
				.title("Substitution assigned")
				.message("You have been asked to cover a class on " + date + ".")
				.priority(NotificationPriority.NORMAL)
				.build());
	}
}
