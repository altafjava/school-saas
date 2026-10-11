package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.teacher.model.Teacher;

/**
 * Verifies that teacher records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class TeacherTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "tch-a-" + suffix, 1L, "admin@tch-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "tch-b-" + suffix, 1L, "admin@tch-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void teacherHiredUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		String code = "EMP-" + UUID.randomUUID().toString().substring(0, 6);
		teacherService.hire(code, "Alice", "Smith", "alice@a.edu", LocalDate.of(2020, 1, 1));

		activateTenant(tenantB);
		Page<Teacher> tenantBTeachers = teacherService.searchTeachers(PageRequest.of(0, 100), null);

		boolean found = tenantBTeachers.getContent().stream()
				.anyMatch(t -> tenantA.getId().equals(t.getTenantId()));
		assertFalse(found, "Tenant B must not see teachers hired under tenant A");
	}

	@Test
	void teacherPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		String code = "EMP-" + UUID.randomUUID().toString().substring(0, 6);
		Teacher teacher = teacherService.hire(code, "Bob", "Jones", "bob@a.edu", LocalDate.of(2019, 3, 20));
		String publicId = teacher.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> teacherService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's teacher");
	}
}
