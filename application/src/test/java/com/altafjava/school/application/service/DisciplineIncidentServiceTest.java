package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.discipline.model.DisciplineIncident;
import com.altafjava.school.domain.discipline.model.IncidentSeverity;
import com.altafjava.school.domain.discipline.repository.DisciplineIncidentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class DisciplineIncidentServiceTest {

	private static final Long CURRENT_USER_ID = 55L;

	@Mock
	private DisciplineIncidentRepository disciplineIncidentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	private final ActivityLogService activityLogService = new NoOpActivityLogService();

	private DisciplineIncidentService disciplineIncidentService;

	@BeforeEach
	void setUp() {
		disciplineIncidentService = new DisciplineIncidentService(disciplineIncidentRepository, studentRepository,
				teacherRepository, recipientResolver, notificationService, studentDataAccessGuard, activityLogService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	@Test
	void record_resolvesTeacherAndNotifiesGuardian() {
		authenticateAsUser(CURRENT_USER_ID);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(10L);
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(20L);
		when(studentRepository.findByPublicIdAndTenantId(any(), eq(1L)))
				.thenReturn(Optional.of(student));
		when(teacherRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(teacher));
		when(disciplineIncidentRepository.save(any(DisciplineIncident.class))).thenAnswer(inv -> inv.getArgument(0));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(99L));

		DisciplineIncident incident = assertDoesNotThrow(() -> disciplineIncidentService.record(
				UUID.randomUUID().toString(), LocalDate.of(2026, 5, 1), IncidentSeverity.MAJOR,
				"Fighting"));

		assertEquals(20L, incident.getReportedByTeacherId());
		assertTrue(incident.isGuardianNotified());
		verify(notificationService, times(1)).send(any(SendNotificationCommand.class));
	}

	@Test
	void recordAction_updatesActionTaken() {
		UUID publicId = UUID.randomUUID();
		DisciplineIncident incident = DisciplineIncident.report(10L, 20L, LocalDate.of(2026, 5, 1),
				IncidentSeverity.MINOR, "Late to class");
		when(disciplineIncidentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(incident));
		when(disciplineIncidentRepository.save(any(DisciplineIncident.class))).thenAnswer(inv -> inv.getArgument(0));

		DisciplineIncident updated = disciplineIncidentService.recordAction(publicId.toString(), "Verbal warning");

		assertEquals("Verbal warning", updated.getActionTaken());
	}
}
