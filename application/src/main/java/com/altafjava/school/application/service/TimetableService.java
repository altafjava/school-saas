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
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;

@Service
public class TimetableService {

	private final TimetableEntryRepository timetableEntryRepository;
	private final PeriodRepository periodRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final TeacherRepository teacherRepository;

	public TimetableService(TimetableEntryRepository timetableEntryRepository, PeriodRepository periodRepository,
			ClassroomRepository classroomRepository, SubjectRepository subjectRepository,
			TeacherRepository teacherRepository) {
		this.timetableEntryRepository = timetableEntryRepository;
		this.periodRepository = periodRepository;
		this.classroomRepository = classroomRepository;
		this.subjectRepository = subjectRepository;
		this.teacherRepository = teacherRepository;
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
	public TimetableEntry schedule(DayOfWeek dayOfWeek, Long periodId, Long classroomId, Long subjectId,
			Long teacherId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!periodRepository.existsByIdAndTenantId(periodId, tenantId)) {
			throw new ResourceNotFoundException("Period not found: " + periodId);
		}
		if (!classroomRepository.existsByIdAndTenantId(classroomId, tenantId)) {
			throw new ResourceNotFoundException("Classroom not found: " + classroomId);
		}
		if (!subjectRepository.existsByIdAndTenantId(subjectId, tenantId)) {
			throw new ResourceNotFoundException("Subject not found: " + subjectId);
		}
		if (!teacherRepository.existsByIdAndTenantIdAndStatus(teacherId, tenantId, EmployeeStatus.ACTIVE)) {
			throw new ResourceNotFoundException("Teacher not found: " + teacherId);
		}
		if (timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(tenantId, dayOfWeek,
				periodId, classroomId)) {
			throw new BusinessException(
					"Classroom " + classroomId + " already has a timetable entry for " + dayOfWeek + " period "
							+ periodId);
		}
		if (timetableEntryRepository.existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(tenantId, dayOfWeek,
				periodId, teacherId)) {
			throw new BusinessException(
					"Teacher " + teacherId + " is already scheduled for " + dayOfWeek + " period " + periodId);
		}
		TimetableEntry entry = TimetableEntry.create(dayOfWeek, periodId, classroomId, subjectId, teacherId);
		return timetableEntryRepository.save(entry);
	}
}
