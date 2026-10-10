package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.filter.AttendanceFilter;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;
import com.altafjava.school.domain.attendance.repository.PeriodAttendanceRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;

@Service
public class PeriodAttendanceService {

	private final PeriodAttendanceRepository periodAttendanceRepository;
	private final StudentRepository studentRepository;
	private final TimetableEntryRepository timetableEntryRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final AcademicScopeResolver academicScopeResolver;
	private final AcademicAccessGuard academicAccessGuard;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final PublicIdLookup publicIdLookup;

	public PeriodAttendanceService(PeriodAttendanceRepository periodAttendanceRepository,
			StudentRepository studentRepository, TimetableEntryRepository timetableEntryRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository,
			AcademicScopeResolver academicScopeResolver, AcademicAccessGuard academicAccessGuard,
			StudentDataAccessGuard studentDataAccessGuard, PublicIdLookup publicIdLookup) {
		this.publicIdLookup = publicIdLookup;
		this.periodAttendanceRepository = periodAttendanceRepository;
		this.studentRepository = studentRepository;
		this.timetableEntryRepository = timetableEntryRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.academicScopeResolver = academicScopeResolver;
		this.academicAccessGuard = academicAccessGuard;
		this.studentDataAccessGuard = studentDataAccessGuard;
	}

	@Transactional(readOnly = true)
	public Page<PeriodAttendance> listAttendance(AttendanceFilter filter, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicScope scope = academicScopeResolver.current(tenantId);
		return periodAttendanceRepository.search(tenantId, scope.readsAllClassrooms(),
				scope.teaching().classroomIds(), scope.ownStudentIds(),
				publicIdLookup.idOrNull(EntityRef.CLASSROOM, filter.classroomPublicId()),
				publicIdLookup.idOrNull(EntityRef.STUDENT, filter.studentPublicId()), filter.dates().from(),
				filter.dates().to(), filter.status(), pageable);
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

	// The classroom comes from the timetable entry, so a caller cannot pair an entry with another classroom.
	@Transactional
	public PeriodAttendance mark(String studentPublicId, String timetableEntryPublicId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long studentId = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId))
				.getId();
		TimetableEntry timetableEntry = timetableEntryRepository
				.findByPublicIdAndTenantId(UUID.fromString(timetableEntryPublicId), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Timetable entry not found: " + timetableEntryPublicId));
		Long classroomId = timetableEntry.getClassroomId();
		if (studentClassroomLinkRepository.findByStudentIdAndClassroomId(tenantId, studentId, classroomId)
				.isEmpty()) {
			throw new ResourceNotFoundException(
					"Student " + studentPublicId + " is not enrolled in the classroom of this timetable entry");
		}
		academicAccessGuard.assertCanMarkPeriod(tenantId, timetableEntry, attendanceDate);
		if (periodAttendanceRepository.existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(studentId,
				timetableEntry.getId(), attendanceDate, tenantId)) {
			throw new IllegalArgumentException("Period attendance already marked for student " + studentPublicId
					+ " for timetable entry " + timetableEntryPublicId + " on " + attendanceDate);
		}
		PeriodAttendance attendance = PeriodAttendance.create(studentId, classroomId, timetableEntry.getId(),
				attendanceDate, status, markedBy);
		return periodAttendanceRepository.save(attendance);
	}
}
