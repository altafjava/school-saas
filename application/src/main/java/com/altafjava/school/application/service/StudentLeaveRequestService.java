package com.altafjava.school.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.filter.StudentLeaveRequestFilter;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceCorrection;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceCorrectionRepository;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.StudentLeaveRequest;
import com.altafjava.school.domain.leave.repository.StudentLeaveRequestRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * A guardian or staff member asks for a student's absence; the class teacher decides. Approval
 * writes the excused days straight to the register, so the classroom sheet reflects it at once.
 */
@Service
@RequiredArgsConstructor
public class StudentLeaveRequestService {

	private static final String MANAGE_PERMISSION = "STUDENT_LEAVE_MANAGE";
	private static final String MARKED_BY = "student-leave";
	private static final Set<LeaveRequestStatus> BLOCKING = Set.of(LeaveRequestStatus.PENDING,
			LeaveRequestStatus.APPROVED);

	private final StudentLeaveRequestRepository studentLeaveRequestRepository;
	private final StudentRepository studentRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final ClassroomRepository classroomRepository;
	private final AcademicYearRepository academicYearRepository;
	private final AttendanceRepository attendanceRepository;
	private final AttendanceCorrectionRepository attendanceCorrectionRepository;
	private final HolidayService holidayService;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final AcademicScopeResolver academicScopeResolver;
	private final PermissionAuthorizationService permissionAuthorizationService;
	private final PublicIdLookup publicIdLookup;

	@Transactional
	public StudentLeaveRequest submit(String studentPublicId, LocalDate startDate, LocalDate endDate, String reason,
			String attachmentFilePublicId, Long requestedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		if (studentLeaveRequestRepository.existsOverlapping(tenantId, student.getId(), BLOCKING, startDate, endDate)) {
			throw new BusinessException("This student already has a leave request covering these dates");
		}
		UUID attachment = attachmentFilePublicId == null ? null : UUID.fromString(attachmentFilePublicId);
		StudentLeaveRequest request = StudentLeaveRequest.submit(student.getId(), currentClassroomId(tenantId, student),
				requestedByUserId, startDate, endDate, reason, attachment);
		return studentLeaveRequestRepository.save(request);
	}

	private Long currentClassroomId(Long tenantId, Student student) {
		return academicYearRepository.findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(tenantId)
				.flatMap(year -> studentClassroomLinkRepository.findByStudentIdAndAcademicYearId(tenantId,
						student.getId(), year.getId()))
				.map(StudentClassroomLink::getClassroomId)
				.orElseThrow(() -> new BusinessException("This student has no classroom in the current academic year"));
	}

	@Transactional(readOnly = true)
	public Page<StudentLeaveRequest> list(StudentLeaveRequestFilter filter, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicScope scope = academicScopeResolver.current(tenantId);
		boolean everyClassroom = scope.readsAllClassrooms()
				|| permissionAuthorizationService.hasPermission(MANAGE_PERMISSION);
		return studentLeaveRequestRepository.search(tenantId, everyClassroom, scope.teaching().classroomIds(),
				scope.ownStudentIds(), publicIdLookup.idOrNull(EntityRef.STUDENT, filter.studentPublicId()),
				publicIdLookup.idOrNull(EntityRef.CLASSROOM, filter.classroomPublicId()), filter.status(),
				filter.dates().from(), filter.dates().to(), pageable);
	}

	@Transactional(readOnly = true)
	public StudentLeaveRequest findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		StudentLeaveRequest request = require(tenantId, publicId);
		assertCanView(tenantId, request);
		return request;
	}

	@Transactional
	public StudentLeaveRequest approve(String publicId, Long decidedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		StudentLeaveRequest request = require(tenantId, publicId);
		assertMayDecide(tenantId, request, decidedByUserId);
		request.approve(decidedByUserId);
		excuseDays(tenantId, request);
		return studentLeaveRequestRepository.save(request);
	}

	@Transactional
	public StudentLeaveRequest reject(String publicId, String rejectionReason, Long decidedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		StudentLeaveRequest request = require(tenantId, publicId);
		assertMayDecide(tenantId, request, decidedByUserId);
		request.reject(decidedByUserId, rejectionReason);
		return studentLeaveRequestRepository.save(request);
	}

	@Transactional
	public StudentLeaveRequest cancel(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		StudentLeaveRequest request = require(tenantId, publicId);
		if (!actorUserId.equals(request.getRequestedByUserId())
				&& !permissionAuthorizationService.hasPermission(MANAGE_PERMISSION)) {
			throw new AccessDeniedException("Only the requester or a leave administrator can cancel this request");
		}
		request.cancel();
		return studentLeaveRequestRepository.save(request);
	}

	// Present and late marks stand: the student was in school, so a leave cannot overwrite that.
	private void excuseDays(Long tenantId, StudentLeaveRequest request) {
		for (LocalDate day : schoolDays(tenantId, request)) {
			attendanceRepository.findByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(request.getStudentId(),
					request.getClassroomId(), day, tenantId)
					.ifPresentOrElse(this::excuseIfAbsent, () -> attendanceRepository.save(Attendance.create(
							request.getStudentId(), request.getClassroomId(), day, AttendanceStatus.EXCUSED,
							MARKED_BY)));
		}
	}

	private void excuseIfAbsent(Attendance attendance) {
		if (attendance.getStatus() != AttendanceStatus.ABSENT) {
			return;
		}
		attendanceCorrectionRepository
				.save(AttendanceCorrection.record(attendance.getId(), AttendanceStatus.ABSENT,
						AttendanceStatus.EXCUSED));
		attendance.updateStatus(AttendanceStatus.EXCUSED);
		attendanceRepository.save(attendance);
	}

	// Sunday is off everywhere this product runs; a school's other days off are in its holiday calendar.
	private List<LocalDate> schoolDays(Long tenantId, StudentLeaveRequest request) {
		Set<LocalDate> holidays = holidayService.datesInRange(tenantId, request.getStartDate(), request.getEndDate());
		return request.getStartDate().datesUntil(request.getEndDate().plusDays(1))
				.filter(day -> day.getDayOfWeek() != DayOfWeek.SUNDAY && !holidays.contains(day))
				.toList();
	}

	private void assertCanView(Long tenantId, StudentLeaveRequest request) {
		AcademicScope scope = academicScopeResolver.current(tenantId);
		if (scope.canReadClassroom(request.getClassroomId()) || scope.ownsStudent(request.getStudentId())
				|| permissionAuthorizationService.hasPermission(MANAGE_PERMISSION)) {
			return;
		}
		throw new AccessDeniedException("Not authorized to view this leave request");
	}

	private void assertMayDecide(Long tenantId, StudentLeaveRequest request, Long actorUserId) {
		if (actorUserId.equals(request.getRequestedByUserId())) {
			throw new AccessDeniedException("You cannot decide a leave request you raised");
		}
		if (permissionAuthorizationService.hasPermission(MANAGE_PERMISSION) || leadsClassroom(tenantId, request)) {
			return;
		}
		throw new AccessDeniedException("This leave request is awaiting the student's class teacher");
	}

	private boolean leadsClassroom(Long tenantId, StudentLeaveRequest request) {
		Long teacherId = academicScopeResolver.current(tenantId).teaching().teacherId();
		return teacherId != null && classroomRepository.findByIdAndTenantId(request.getClassroomId(), tenantId)
				.map(Classroom::getClassTeacherId).filter(teacherId::equals).isPresent();
	}

	private StudentLeaveRequest require(Long tenantId, String publicId) {
		return studentLeaveRequestRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + publicId));
	}
}
