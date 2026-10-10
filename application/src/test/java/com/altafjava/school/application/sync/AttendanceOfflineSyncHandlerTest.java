package com.altafjava.school.application.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.sync.EntityChange;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdResolver;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.TeachingAssignments;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AttendanceOfflineSyncHandlerTest {

	private static final Long TENANT_ID = 1L;
	private static final AcademicScope ALL_CLASSROOMS = new AcademicScope(true, true, TeachingAssignments.NONE,
			Set.of());

	@Mock
	private AttendanceService attendanceService;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private AcademicScopeResolver academicScopeResolver;
	@Mock
	private PublicIdResolver publicIdResolver;

	private AttendanceOfflineSyncHandler handler;

	@BeforeEach
	void setUp() {
		handler = new AttendanceOfflineSyncHandler(attendanceService, attendanceRepository,
				JsonMapper.builder().build(), academicScopeResolver, publicIdResolver);
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearPrincipal() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void create_marksAsTheSignedInUser_ignoringAnyMarkerTheDeviceSends() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("teacher-a", null, List.of()));
		String studentPublicId = UUID.randomUUID().toString();
		String classroomPublicId = UUID.randomUUID().toString();
		Attendance created = Attendance.create(10L, 20L, LocalDate.of(2026, 1, 15), AttendanceStatus.PRESENT,
				"teacher-a");
		UUID attendancePublicId = UUID.randomUUID();
		created.setPublicId(attendancePublicId);
		when(attendanceService.mark(studentPublicId, classroomPublicId, LocalDate.of(2026, 1, 15),
				AttendanceStatus.PRESENT, "teacher-a")).thenReturn(created);

		String payload = "{\"studentPublicId\":\"" + studentPublicId + "\",\"classroomPublicId\":\""
				+ classroomPublicId + "\",\"attendanceDate\":\"2026-01-15\",\"status\":\"PRESENT\","
				+ "\"markedBy\":\"someone-else\"}";

		assertEquals(attendancePublicId, handler.create(payload));
	}

	@Test
	void create_withMalformedPayload_throwsBusinessExceptionNotRawJacksonException() {
		assertThrows(BusinessException.class, () -> handler.create("not valid json"));
	}

	@Test
	void update_delegatesToAttendanceServiceUpdateStatus() {
		UUID entityId = UUID.randomUUID();

		handler.update(entityId, "{\"status\":\"ABSENT\"}");

		org.mockito.Mockito.verify(attendanceService).updateStatus(entityId.toString(), AttendanceStatus.ABSENT);
	}

	@Test
	void delete_delegatesToAttendanceServiceDelete() {
		UUID entityId = UUID.randomUUID();

		handler.delete(entityId);

		org.mockito.Mockito.verify(attendanceService).delete(entityId.toString());
	}

	@Test
	void findChange_returnsCurrentStateWithPublicIdsOnlyInPayload() {
		UUID entityId = UUID.randomUUID();
		Attendance attendance = Attendance.create(10L, 20L, LocalDate.of(2026, 1, 15), AttendanceStatus.PRESENT,
				"teacher-a");
		attendance.setPublicId(entityId);
		when(attendanceRepository.findByPublicIdAndTenantId(entityId, TENANT_ID)).thenReturn(Optional.of(attendance));
		when(academicScopeResolver.current(TENANT_ID)).thenReturn(ALL_CLASSROOMS);
		when(publicIdResolver.resolve(EntityRef.STUDENT, 10L)).thenReturn("student-public-id");

		Optional<EntityChange> result = handler.findChange(entityId);

		assertTrue(result.isPresent());
		assertEquals(entityId, result.get().entityId());
		assertTrue(result.get().payloadJson().contains("\"studentPublicId\":\"student-public-id\""));
		assertFalse(result.get().payloadJson().contains("\"studentId\""));
	}

	@Test
	void changesSince_mapsEveryAttendanceRowToAnEntityChange() {
		Attendance a1 = Attendance.create(10L, 20L, LocalDate.of(2026, 1, 15), AttendanceStatus.PRESENT, "t");
		a1.setPublicId(UUID.randomUUID());
		Attendance a2 = Attendance.create(11L, 20L, LocalDate.of(2026, 1, 15), AttendanceStatus.ABSENT, "t");
		a2.setPublicId(UUID.randomUUID());
		Instant since = Instant.now().minusSeconds(60);
		when(attendanceRepository.findByTenantIdAndUpdatedAtAfter(TENANT_ID, since)).thenReturn(List.of(a1, a2));
		when(academicScopeResolver.current(TENANT_ID)).thenReturn(ALL_CLASSROOMS);

		List<EntityChange> result = handler.changesSince(since);

		assertEquals(2, result.size());
	}

	@Test
	void findChange_recordOutsideCallersScope_isHidden() {
		UUID entityId = UUID.randomUUID();
		Attendance attendance = Attendance.create(10L, 20L, LocalDate.of(2026, 1, 15), AttendanceStatus.PRESENT,
				"teacher-a");
		when(attendanceRepository.findByPublicIdAndTenantId(entityId, TENANT_ID)).thenReturn(Optional.of(attendance));
		when(academicScopeResolver.current(TENANT_ID)).thenReturn(AcademicScope.NONE);

		assertTrue(handler.findChange(entityId).isEmpty());
	}

	@Test
	void changesSince_asTeacher_pullsOnlyTaughtClassroomsAndOwnStudents() {
		Instant since = Instant.now().minusSeconds(60);
		TeachingAssignments teaching = new TeachingAssignments(70L, Set.of(20L), Map.of());
		when(academicScopeResolver.current(TENANT_ID))
				.thenReturn(new AcademicScope(false, false, teaching, Set.of()));
		when(attendanceRepository.findVisibleUpdatedAfter(TENANT_ID, since, Set.of(20L), Set.of()))
				.thenReturn(List.of());

		assertTrue(handler.changesSince(since).isEmpty());

		verify(attendanceRepository, never()).findByTenantIdAndUpdatedAtAfter(TENANT_ID, since);
	}
}
