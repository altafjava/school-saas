package com.altafjava.school.application.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.sync.EntityChange;
import com.altafjava.platform.core.sync.OfflineSyncEntityHandler;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdResolver;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Offline sync for attendance, the case where a teacher marks a class with poor connectivity.
 * Every write goes through {@link AttendanceService}, so a synced write obeys the same rules as an
 * online one, and every payload names students and classrooms by public id only.
 */
@Component
@RequiredArgsConstructor
public class AttendanceOfflineSyncHandler implements OfflineSyncEntityHandler {

	private final AttendanceService attendanceService;
	private final AttendanceRepository attendanceRepository;
	private final ObjectMapper objectMapper;
	private final AcademicScopeResolver academicScopeResolver;
	private final PublicIdResolver publicIdResolver;

	@Override
	public UUID create(String payloadJson) {
		CreatePayload payload = readValue(payloadJson, CreatePayload.class);
		return attendanceService.mark(payload.studentPublicId(), payload.classroomPublicId(),
				payload.attendanceDate(), payload.status(), currentUsername()).getPublicId();
	}

	// The marker is the signed-in user, never a name the device sends.
	private String currentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? null : authentication.getName();
	}

	@Override
	public void update(UUID entityId, String payloadJson) {
		UpdatePayload payload = readValue(payloadJson, UpdatePayload.class);
		attendanceService.updateStatus(entityId.toString(), payload.status(), ExpectedVersion.any());
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
		OutputPayload payload = new OutputPayload(
				publicIdResolver.resolve(EntityRef.STUDENT, attendance.getStudentId()),
				publicIdResolver.resolve(EntityRef.CLASSROOM, attendance.getClassroomId()),
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
			AttendanceStatus status) {
	}

	private record UpdatePayload(AttendanceStatus status) {
	}

	private record OutputPayload(String studentPublicId, String classroomPublicId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
	}
}
