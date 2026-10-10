package com.altafjava.school.application.service;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.model.Venue;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.domain.timetable.repository.VenueRepository;

@Service
public class TimetableService {

	private final TimetableEntryRepository timetableEntryRepository;
	private final PeriodRepository periodRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final TeacherRepository teacherRepository;
	private final VenueRepository venueRepository;

	public TimetableService(TimetableEntryRepository timetableEntryRepository, PeriodRepository periodRepository,
			ClassroomRepository classroomRepository, SubjectRepository subjectRepository,
			TeacherRepository teacherRepository, VenueRepository venueRepository) {
		this.timetableEntryRepository = timetableEntryRepository;
		this.periodRepository = periodRepository;
		this.classroomRepository = classroomRepository;
		this.subjectRepository = subjectRepository;
		this.teacherRepository = teacherRepository;
		this.venueRepository = venueRepository;
	}

	@Transactional(readOnly = true)
	public Page<TimetableEntry> listEntries(Pageable pageable) {
		return timetableEntryRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public List<TimetableEntry> listForClassroom(String classroomPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long classroomId = classroomRepository.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId))
				.getId();
		return timetableEntryRepository.findAllByTenantIdAndClassroomId(tenantId, classroomId);
	}

	@Transactional(readOnly = true)
	public TimetableEntry findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return timetableEntryRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found: " + publicId));
	}

	@Transactional
	public TimetableEntry schedule(DayOfWeek dayOfWeek, String periodPublicId, String classroomPublicId,
			String subjectPublicId, String teacherPublicId, String venuePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long periodId = periodRepository.findByPublicIdAndTenantId(UUID.fromString(periodPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodPublicId))
				.getId();
		Long classroomId = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId))
				.getId();
		Long subjectId = subjectRepository.findByPublicIdAndTenantId(UUID.fromString(subjectPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectPublicId))
				.getId();
		Long teacherId = teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
				.filter(teacher -> teacher.getStatus() == EmployeeStatus.ACTIVE)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherPublicId))
				.getId();
		if (timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(tenantId, dayOfWeek,
				periodId, classroomId)) {
			throw new BusinessException("The classroom already has a timetable entry for " + dayOfWeek
					+ " in this period");
		}
		if (timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(tenantId, dayOfWeek,
				periodId, teacherId)) {
			throw new BusinessException("The teacher is already scheduled for " + dayOfWeek + " in this period");
		}
		Long venueId = venuePublicId == null ? null : requireFreeVenue(tenantId, venuePublicId, dayOfWeek, periodId);
		TimetableEntry entry = TimetableEntry.create(dayOfWeek, periodId, classroomId, subjectId, teacherId, venueId);
		return timetableEntryRepository.save(entry);
	}

	@Transactional
	public TimetableEntry assignVenue(String publicId, String venuePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		TimetableEntry entry = findByPublicId(publicId);
		entry.assignVenue(requireFreeVenue(tenantId, venuePublicId, entry.getDayOfWeek(), entry.getPeriodId(),
				entry.getId()));
		return timetableEntryRepository.save(entry);
	}

	private Long requireFreeVenue(Long tenantId, String venuePublicId, DayOfWeek dayOfWeek, Long periodId) {
		return requireFreeVenue(tenantId, venuePublicId, dayOfWeek, periodId, null);
	}

	// The slot being moved is allowed to already hold the venue it is being assigned.
	private Long requireFreeVenue(Long tenantId, String venuePublicId, DayOfWeek dayOfWeek, Long periodId,
			Long movingEntryId) {
		Venue venue = venueRepository.findByPublicIdAndTenantId(UUID.fromString(venuePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + venuePublicId));
		if (!venue.isActive()) {
			throw new BusinessException("Venue " + venue.getName() + " is not in use");
		}
		boolean bookedByAnotherSlot = timetableEntryRepository
				.findAllByTenantIdAndDayOfWeekAndPeriodId(tenantId, dayOfWeek, periodId).stream()
				.anyMatch(slot -> venue.getId().equals(slot.getVenueId()) && !slot.getId().equals(movingEntryId));
		if (bookedByAnotherSlot) {
			throw new BusinessException(
					"Venue " + venue.getName() + " is already booked on " + dayOfWeek + " period " + periodId);
		}
		return venue.getId();
	}
}
