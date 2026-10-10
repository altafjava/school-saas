package com.altafjava.school.application.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.sync.EntityChange;
import com.altafjava.platform.core.sync.OfflineSyncEntityHandler;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * The first real, registered {@link OfflineSyncEntityHandler} — attendance is the offline-critical
 * case (a teacher marking attendance in a classroom with poor connectivity), and {@link
 * AttendanceService#mark} already enforces roster membership, so a synced write runs through the
 * exact same business rules an online request would, not a bypass. Every write delegates to
 * {@link AttendanceService} rather than touching {@link AttendanceRepository} directly.
 *
 * <p>
 * {@code create}'s payload references the student/classroom by their public ids (all a client can
 * know); {@code changesSince}/{@code findChange}'s output payload uses internal ids instead, to
 * avoid an extra publicId lookup per row on a bulk delta pull — a real, documented asymmetry, not
 * an oversight.
 */
@Component
@RequiredArgsConstructor
public class AttendanceOfflineSyncHandler implements OfflineSyncEntityHandler {

	private final AttendanceService attendanceService;
	private final AttendanceRepository attendanceRepository;
	private final StudentRepository studentRepository;
	private final ClassroomRepository classroomRepository;
	private final ObjectMapper objectMapper;
	private final AcademicScopeResolver academicScopeResolver;

	@Override
	public UUID create(String payloadJson) {
		CreatePayload payload = readValue(payloadJson, CreatePayload.class);
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository
				.findByPublicIdAndTenantId(UUID.fromString(payload.studentPublicId()), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + payload.studentPublicId()));
		Classroom classroom = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(payload.classroomPublicId()), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Classroom not found: " + payload.classroomPublicId()));
		Attendance attendance = attendanceService.mark(student.getId(), classroom.getId(), payload.attendanceDate(),
				payload.status(), payload.markedBy());
		return attendance.getPublicId();
	}

	@Override
	public void update(UUID entityId, String payloadJson) {
		UpdatePayload payload = readValue(payloadJson, UpdatePayload.class);
		attendanceService.updateStatus(entityId.toString(), payload.status());
	}

	@Override
	public void delete(UUID entityId) {
		attendanceService.delete(entityId.toString());
	}

	@Override
	public Optional<EntityChange> findChange(UUID entityId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicScope scope = academicScopeResolver.current(tenantId);
		return attendanceRepository.findByPublicIdAndTenantId(entityId, tenantId)
				.filter(attendance -> scope.canReadClassroom(attendance.getClassroomId())
						|| scope.ownsStudent(attendance.getStudentId()))
				.map(this::toChange);
	}

	@Override
	public List<EntityChange> changesSince(Instant since) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicScope scope = academicScopeResolver.current(tenantId);
		List<Attendance> changed = scope.readsAllClassrooms()
				? attendanceRepository.findByTenantIdAndUpdatedAtAfter(tenantId, since)
				: attendanceRepository.findVisibleUpdatedAfter(tenantId, since, scope.teaching().classroomIds(),
						scope.ownStudentIds());
		return changed.stream().map(this::toChange).toList();
	}

	private EntityChange toChange(Attendance attendance) {
		OutputPayload payload = new OutputPayload(attendance.getStudentId(), attendance.getClassroomId(),
				attendance.getAttendanceDate(), attendance.getStatus(), attendance.getMarkedBy());
		return EntityChange.withoutVectorClock(attendance.getPublicId(), attendance.getUpdatedAt(),
				attendance.isDeleted(), writeValue(payload));
	}

	private <T> T readValue(String json, Class<T> type) {
		try {
			return objectMapper.readValue(json, type);
		} catch (JacksonException e) {
			throw new BusinessException("Invalid sync payload for attendance: " + e.getMessage());
		}
	}

	private String writeValue(Object value) {
		return objectMapper.writeValueAsString(value);
	}

	private record CreatePayload(String studentPublicId, String classroomPublicId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
	}

	private record UpdatePayload(AttendanceStatus status) {
	}

	private record OutputPayload(Long studentId, Long classroomId, LocalDate attendanceDate, AttendanceStatus status,
			String markedBy) {
	}
}
