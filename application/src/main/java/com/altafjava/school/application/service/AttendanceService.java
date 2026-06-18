package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;

@Service
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;

	public AttendanceService(AttendanceRepository attendanceRepository) {
		this.attendanceRepository = attendanceRepository;
	}

	@Transactional(readOnly = true)
	public Page<Attendance> listAttendance(Pageable pageable) {
		return attendanceRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Attendance findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return attendanceRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + publicId));
	}

	@Transactional
	public Attendance mark(Long studentId, Long classroomId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (attendanceRepository.existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(
				studentId, classroomId, attendanceDate, tenantId)) {
			throw new IllegalArgumentException(
					"Attendance already marked for student " + studentId + " on " + attendanceDate);
		}
		Attendance attendance = Attendance.create(studentId, classroomId, attendanceDate, status, markedBy);
		return attendanceRepository.save(attendance);
	}
}
