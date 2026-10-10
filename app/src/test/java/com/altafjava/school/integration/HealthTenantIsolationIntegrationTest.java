package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.HealthRecordService;
import com.altafjava.school.application.service.MedicalIncidentService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that health records and medical incidents created under tenant A are not visible or
 * actionable from tenant B — this data is more sensitive than ordinary operational data
 * ({@code @Pii}-flagged), so isolation matters even more than usual.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class HealthTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private HealthRecordService healthRecordService;

	@Autowired
	private MedicalIncidentService medicalIncidentService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"Health School A", "health-a-" + suffix, 1L, "admin@health-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Health School B", "health-b-" + suffix, 1L, "admin@health-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	@Test
	void healthRecordCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice",
				"Smith", "alice@health.test", LocalDate.of(2010, 1, 1));
		healthRecordService.upsert(student.getPublicId().toString(), "O+", "Peanuts", "Asthma", "MMR",
				ExpectedVersion.any());
		String studentPublicId = student.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> healthRecordService.getByStudent(studentPublicId),
				"Tenant B must not resolve tenant A's student at all");
	}

	@Test
	void medicalIncidentCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		authenticateAsUser(55L);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Bob",
				"Jones", "bob@health.test", LocalDate.of(2011, 2, 2));
		medicalIncidentService.record(student.getPublicId().toString(), LocalDateTime.of(2026, 5, 1, 10, 0),
				"Fell during PE", "Ice pack applied");
		String studentPublicId = student.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> medicalIncidentService.listForStudent(studentPublicId, PageRequest.of(0, 20)),
				"Tenant B must not resolve tenant A's student at all");
	}
}
