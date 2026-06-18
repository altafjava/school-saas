package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.request.MarkAttendanceRequest;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.api.mapper.AttendanceMapper;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

	private final AttendanceService attendanceService;
	private final AttendanceMapper attendanceMapper;

	public AttendanceController(AttendanceService attendanceService, AttendanceMapper attendanceMapper) {
		this.attendanceService = attendanceService;
		this.attendanceMapper = attendanceMapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public Page<AttendanceResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return attendanceService.listAttendance(PageRequest.of(page, Math.min(size, 100)))
				.map(attendanceMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public AttendanceResponse get(@PathVariable String publicId) {
		return attendanceMapper.toResponse(attendanceService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public AttendanceResponse mark(@Valid @RequestBody MarkAttendanceRequest request) {
		return attendanceMapper.toResponse(attendanceService.mark(
				request.studentId(),
				request.classroomId(),
				request.attendanceDate(),
				AttendanceStatus.valueOf(request.status()),
				request.markedBy()));
	}
}
