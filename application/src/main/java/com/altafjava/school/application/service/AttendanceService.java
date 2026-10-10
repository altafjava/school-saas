package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.filter.AttendanceFilter;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceCorrection;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceCorrectionRepository;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.attendance.service.AttendancePercentageCalculator;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;
	private final AttendanceCorrectionRepository attendanceCorrectionRepository;
	private final StudentRepository studentRepository;
	private final ClassroomRepository classroomRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final AcademicScopeResolver academicScopeResolver;
	private final AcademicAccessGuard academicAccessGuard;
	private final HolidayService holidayService;
	private final AttendancePercentageCalculator attendancePercentageCalculator = new AttendancePercentageCalculator();
	private final PublicIdLookup publicIdLookup;

	public AttendanceService(AttendanceRepository attendanceRepository,
			AttendanceCorrectionRepository attendanceCorrectionRepository, StudentRepository studentRepository,
			ClassroomRepository classroomRepository, StudentClassroomLinkRepository studentClassroomLinkRepository,
			StudentDataAccessGuard studentDataAccessGuard,
			AcademicScopeResolver academicScopeResolver, AcademicAccessGuard academicAccessGuard,
			HolidayService holidayService, PublicIdLookup publicIdLookup) {
		this.publicIdLookup = publicIdLookup;
		this.attendanceRepository = attendanceRepository;
		this.attendanceCorrectionRepository = attendanceCorrectionRepository;
		this.studentRepository = studentRepository;
		this.classroomRepository = classroomRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.studentDataAccessGuard = studentDataAccessGuard;
		this.academicScopeResolver = academicScopeResolver;
		this.academicAccessGuard = academicAccessGuard;
		this.holidayService = holidayService;
	}

	// Narrowed to the caller's scope (every classroom, the classrooms they teach, or their own students), then
	// by the filter.
	@Transactional(readOnly = true)
	public Page<Attendance> listAttendance(AttendanceFilter filter, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicScope scope = academicScopeResolver.current(tenantId);
		return attendanceRepository.search(tenantId, scope.readsAllClassrooms(), scope.teaching().classroomIds(),
				scope.ownStudentIds(), publicIdLookup.idOrNull(EntityRef.CLASSROOM, filter.classroomPublicId()),
				publicIdLookup.idOrNull(EntityRef.STUDENT, filter.studentPublicId()), filter.dates().from(),
				filter.dates().to(), filter.status(), pageable);
	}

	@Transactional(readOnly = true)
	public Attendance findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Attendance attendance = requireAttendance(tenantId, publicId);
		academicAccessGuard.assertCanReadStudentRecord(tenantId, attendance.getClassroomId(),
				attendance.getStudentId());
		return attendance;
	}

	private Attendance requireAttendance(Long tenantId, String publicId) {
		return attendanceRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public Page<Attendance> getStudentAttendance(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return attendanceRepository.findByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public AttendancePercentage calculatePercentage(String studentPublicId, LocalDate from, LocalDate to) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		// Denominator is days attendance was actually marked, minus any tenant-defined holiday that
		// fell in range — a holiday mistakenly marked as attendance must not count either way.
		Set<LocalDate> holidayDates = holidayService.datesInRange(tenantId, from, to);
		long totalMarkedDays = holidayDates.isEmpty()
				? attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetween(student.getId(), tenantId,
						from, to)
				: attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenExcludingDates(
						student.getId(), tenantId, from, to, holidayDates);
		long presentDays = holidayDates.isEmpty()
				? attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(student.getId(),
						tenantId, from, to, AttendanceStatus.PRESENT)
				: attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatusExcludingDates(
						student.getId(), tenantId, from, to, AttendanceStatus.PRESENT, holidayDates);
		return attendancePercentageCalculator.calculate(presentDays, totalMarkedDays);
	}

	@Transactional
	public Attendance mark(String studentPublicId, String classroomPublicId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long studentId = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId))
				.getId();
		Long classroomId = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId))
				.getId();
		if (studentClassroomLinkRepository.findByStudentIdAndClassroomId(tenantId, studentId, classroomId)
				.isEmpty()) {
			throw new ResourceNotFoundException(
					"Student " + studentPublicId + " is not enrolled in classroom " + classroomPublicId);
		}
		academicAccessGuard.assertCanWriteClassroom(tenantId, classroomId);
		if (attendanceRepository.existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(
				studentId, classroomId, attendanceDate, tenantId)) {
			throw new IllegalArgumentException(
					"Attendance already marked for student " + studentPublicId + " on " + attendanceDate);
		}
		Attendance attendance = Attendance.create(studentId, classroomId, attendanceDate, status, markedBy);
		return attendanceRepository.save(attendance);
	}

	// Records an AttendanceCorrection with the pre-correction status before mutating, so a
	// disputed attendance record is answerable from history data rather than only visible as an
	// opaque updatedAt bump.
	@Transactional
	public Attendance updateStatus(String publicId, AttendanceStatus status, ExpectedVersion expectedVersion) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Attendance attendance = requireAttendance(tenantId, publicId);
		expectedVersion.verify(attendance);
		academicAccessGuard.assertCanWriteClassroom(tenantId, attendance.getClassroomId());
		AttendanceStatus oldStatus = attendance.getStatus();
		if (oldStatus != status) {
			attendanceCorrectionRepository.save(AttendanceCorrection.record(attendance.getId(), oldStatus, status));
		}
		attendance.updateStatus(status);
		return attendanceRepository.save(attendance);
	}

	@Transactional(readOnly = true)
	public Page<AttendanceCorrection> listCorrections(String publicId, Pageable pageable) {
		Attendance attendance = findByPublicId(publicId);
		return attendanceCorrectionRepository.findByAttendanceIdAndTenantId(TenantContext.getCurrentTenantId(),
				attendance.getId(), pageable);
	}

	@Transactional
	public void delete(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Attendance attendance = requireAttendance(tenantId, publicId);
		academicAccessGuard.assertCanWriteClassroom(tenantId, attendance.getClassroomId());
		attendance.softDelete("attendance-deletion");
		attendanceRepository.save(attendance);
	}
}
