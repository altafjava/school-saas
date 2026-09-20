package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.health.model.MedicalIncident;
import com.altafjava.school.domain.health.repository.MedicalIncidentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class MedicalIncidentServiceTest {

	private static final Long CURRENT_USER_ID = 55L;
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private MedicalIncidentRepository medicalIncidentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;
	private final ActivityLogService activityLogService = new NoOpActivityLogService();

	private MedicalIncidentService medicalIncidentService;

	@BeforeEach
	void setUp() {
		medicalIncidentService = new MedicalIncidentService(medicalIncidentRepository, studentRepository,
				recipientResolver, notificationService, activityLogService);
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

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void record_resolvesRecordingUserAndNotifiesGuardian() {
		authenticateAsUser(CURRENT_USER_ID);
		Student student = studentWithId(10L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(medicalIncidentRepository.save(any(MedicalIncident.class))).thenAnswer(inv -> inv.getArgument(0));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(99L));

		MedicalIncident incident = assertDoesNotThrow(() -> medicalIncidentService.record(
				STUDENT_PUBLIC_ID.toString(), LocalDateTime.of(2026, 5, 1, 10, 0), "Fell during PE", "Ice pack"));

		assertEquals(CURRENT_USER_ID, incident.getRecordedByUserId());
		assertTrue(incident.isGuardianNotified());
		verify(notificationService, times(1)).send(any(SendNotificationCommand.class));
	}

	@Test
	void record_noResolvableGuardian_doesNotSendNotification() {
		authenticateAsUser(CURRENT_USER_ID);
		Student student = studentWithId(10L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(medicalIncidentRepository.save(any(MedicalIncident.class))).thenAnswer(inv -> inv.getArgument(0));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.empty());

		MedicalIncident incident = medicalIncidentService.record(STUDENT_PUBLIC_ID.toString(),
				LocalDateTime.of(2026, 5, 1, 10, 0), "Fell during PE", "Ice pack");

		assertFalse(incident.isGuardianNotified());
		verify(notificationService, never()).send(any(SendNotificationCommand.class));
	}

	@Test
	void record_noAuthenticatedPrincipal_throwsAccessDeniedException() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));

		assertThrows(AccessDeniedException.class, () -> medicalIncidentService.record(STUDENT_PUBLIC_ID.toString(),
				LocalDateTime.of(2026, 5, 1, 10, 0), "Fell during PE", "Ice pack"));
	}
}
