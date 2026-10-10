package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalTime;
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
import com.altafjava.school.application.service.PeriodService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.timetable.model.Period;

/**
 * Verifies that period records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class PeriodTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private PeriodService periodService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "prd-a-" + suffix, 1L, "admin@prd-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "prd-b-" + suffix, 1L, "admin@prd-b.test", "Password123!", "USD"));
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
	void periodCreatedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		periodService.create("Period 1", LocalTime.of(9, 0), LocalTime.of(9, 45), 1);

		activateTenant(tenantB);
		Page<Period> tenantBPeriods = periodService.listPeriods(null, PageRequest.of(0, 100));

		boolean found = tenantBPeriods.getContent().stream()
				.anyMatch(p -> tenantA.getId().equals(p.getTenantId()));
		assertFalse(found, "Tenant B must not see periods created under tenant A");
	}

	@Test
	void periodPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		Period period = periodService.create("Period 2", LocalTime.of(10, 0), LocalTime.of(10, 45), 2);
		String publicId = period.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> periodService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's period");
	}

	@Test
	void periodName_uniquenessIsScopedPerTenant() {
		activateTenant(tenantA);
		periodService.create("Period 3", LocalTime.of(11, 0), LocalTime.of(11, 45), 3);

		activateTenant(tenantB);
		Period tenantBPeriod = periodService.create("Period 3", LocalTime.of(11, 0), LocalTime.of(11, 45), 3);

		assertNotNull(tenantBPeriod, "Tenant B must be able to reuse a period name already used by tenant A");
	}
}
