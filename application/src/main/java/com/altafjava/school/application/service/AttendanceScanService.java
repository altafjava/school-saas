package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.application.document.StudentPlacementResolver.Placement;
import com.altafjava.school.application.idcard.QrPayloadReader;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Marks a student present for today from a scan of their ID card's QR code. The card must be a
 * genuine, unrevoked student card of this school. Scanning again on the same day changes nothing,
 * so a student walking past a gate twice is not an error.
 */
@Service
@RequiredArgsConstructor
public class AttendanceScanService {

	private final QrPayloadReader qrPayloadReader;
	private final DocumentIssuanceService documentIssuanceService;
	private final StudentRepository studentRepository;
	private final StudentPlacementResolver placementResolver;
	private final AttendanceRepository attendanceRepository;
	private final HolidayService holidayService;

	@Transactional
	public AttendanceScanResult scan(String qrPayload, String scannedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentHolding(tenantId, qrPayload);
		LocalDate today = LocalDate.now();
		if (!holidayService.datesInRange(tenantId, today, today).isEmpty()) {
			throw new BusinessException("Today is a school holiday — attendance is not taken");
		}
		Placement placement = placementResolver.resolve(tenantId, student.getId())
				.orElseThrow(() -> new BusinessException("The student is not enrolled in any classroom"));
		Classroom classroom = placement.classroom().orElseThrow(
				() -> new ResourceNotFoundException("Classroom not found: " + placement.link().getClassroomId()));

		Optional<Attendance> existing = attendanceRepository.findByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(
				student.getId(), classroom.getId(), today, tenantId);
		if (existing.isPresent()) {
			return new AttendanceScanResult(existing.get(), student, classroom, true);
		}
		Attendance marked = attendanceRepository.save(Attendance.create(student.getId(), classroom.getId(), today,
				AttendanceStatus.PRESENT, "qr-scan:" + scannedBy));
		return new AttendanceScanResult(marked, student, classroom, false);
	}

	private Student studentHolding(Long tenantId, String qrPayload) {
		DocumentIssuance card = documentIssuanceService.verify(tenantId, qrPayloadReader.verificationCodeOf(qrPayload));
		if (!SchoolDocumentTypes.STUDENT_ID_CARD.equals(card.getDocumentType())) {
			throw new BusinessException("Only a student ID card can mark student attendance");
		}
		if (card.isRevoked()) {
			throw new BusinessException("This ID card has been revoked");
		}
		Student student = studentRepository.findByIdAndTenantId(card.getOwnerEntityId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found for this ID card"));
		if (student.getEnrollmentStatus() != EnrollmentStatus.ACTIVE) {
			throw new BusinessException("The cardholder is not currently enrolled");
		}
		return student;
	}
}
