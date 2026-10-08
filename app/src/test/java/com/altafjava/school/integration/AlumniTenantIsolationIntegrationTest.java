package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.lifecycle.LifecycleChange;
import com.altafjava.school.application.service.AlumniProfileService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that an alumni profile created under tenant A is not visible or actionable from tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class AlumniTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private AlumniProfileService alumniProfileService;

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
				"Alumni School A", "alumni-a-" + suffix, 1L, "admin@alumni-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Alumni School B", "alumni-b-" + suffix, 1L, "admin@alumni-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@Test
	void alumniProfileCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice",
				"Smith", "alice@alumni.test", LocalDate.of(2008, 1, 1));
		studentService.graduate(student.getPublicId().toString(), LifecycleChange.NONE);
		var profile = alumniProfileService.create(student.getPublicId().toString(), 2026, "Software Engineer",
				"alice@alumni-contact.test", "555-0100");
		String profilePublicId = profile.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> alumniProfileService.findByPublicId(profilePublicId),
				"Tenant B must not be able to resolve tenant A's alumni profile");
	}
}
