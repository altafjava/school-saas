package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;
import com.altafjava.school.domain.attendance.repository.PeriodAttendanceRepository;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;

@Service
public class PeriodAttendanceService {

	private final PeriodAttendanceRepository periodAttendanceRepository;
	private final StudentRepository studentRepository;
	private final ClassroomRepository classroomRepository;
	private final TimetableEntryRepository timetableEntryRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final AcademicScopeResolver academicScopeResolver;
	private final AcademicAccessGuard academicAccessGuard;
	private final StudentDataAccessGuard studentDataAccessGuard;

	public PeriodAttendanceService(PeriodAttendanceRepository periodAttendanceRepository,
			StudentRepository studentRepository, ClassroomRepository classroomRepository,
			TimetableEntryRepository timetableEntryRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository,
			AcademicScopeResolver academicScopeResolver, AcademicAccessGuard academicAccessGuard,
			StudentDataAccessGuard studentDataAccessGuard) {
		this.periodAttendanceRepository = periodAttendanceRepository;
		this.studentRepository = studentRepository;
		this.classroomRepository = classroomRepository;
		this.timetableEntryRepository = timetableEntryRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.academicScopeResolver = academicScopeResolver;
		this.academicAccessGuard = academicAccessGuard;
		this.studentDataAccessGuard = studentDataAccessGuard;
	}

	@Transactional(readOnly = true)
	public Page<PeriodAttendance> listAttendance(Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicScope scope = academicScopeResolver.current(tenantId);
		if (scope.readsAllClassrooms()) {
			return periodAttendanceRepository.findAllByTenantId(tenantId, pageable);
		}
		return periodAttendanceRepository.findVisible(tenantId, scope.teaching().classroomIds(),
				scope.ownStudentIds(), pageable);
	}

	@Transactional(readOnly = true)
	public PeriodAttendance findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		PeriodAttendance attendance = periodAttendanceRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Period attendance record not found: " + publicId));
		academicAccessGuard.assertCanReadStudentRecord(tenantId, attendance.getClassroomId(),
				attendance.getStudentId());
		return attendance;
	}

	@Transactional(readOnly = true)
	public Page<PeriodAttendance> getStudentAttendance(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return periodAttendanceRepository.findByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional
	public PeriodAttendance mark(Long studentId, Long classroomId, Long timetableEntryId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!studentRepository.existsByIdAndTenantId(studentId, tenantId)) {
			throw new ResourceNotFoundException("Student not found: " + studentId);
		}
		if (!classroomRepository.existsByIdAndTenantId(classroomId, tenantId)) {
			throw new ResourceNotFoundException("Classroom not found: " + classroomId);
		}
		TimetableEntry timetableEntry = timetableEntryRepository.findByIdAndTenantId(timetableEntryId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found: " + timetableEntryId));
		if (!timetableEntry.getClassroomId().equals(classroomId)) {
			throw new BusinessException(
					"Timetable entry " + timetableEntryId + " does not belong to classroom " + classroomId);
		}
		if (studentClassroomLinkRepository.findByStudentIdAndClassroomId(tenantId, studentId, classroomId)
				.isEmpty()) {
			throw new ResourceNotFoundException(
					"Student " + studentId + " is not enrolled in classroom " + classroomId);
		}
		academicAccessGuard.assertCanMarkPeriod(tenantId, timetableEntry, attendanceDate);
		if (periodAttendanceRepository.existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(studentId,
				timetableEntryId, attendanceDate, tenantId)) {
			throw new IllegalArgumentException("Period attendance already marked for student " + studentId
					+ " for timetable entry " + timetableEntryId + " on " + attendanceDate);
		}
		PeriodAttendance attendance = PeriodAttendance.create(studentId, classroomId, timetableEntryId,
				attendanceDate, status, markedBy);
		return periodAttendanceRepository.save(attendance);
	}
}
