package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;

/**
 * Verifies that academic year records created under tenant A are not visible to tenant B, and
 * that the "at most one current year" invariant is scoped per tenant, not global.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class AcademicYearTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "ay-a-" + suffix, 1L, "admin@ay-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "ay-b-" + suffix, 1L, "admin@ay-b.test", "Password123!", "USD"));
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
	void academicYearCreatedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		academicYearService.create("2025-26", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true);

		activateTenant(tenantB);
		Page<AcademicYear> tenantBYears = academicYearService.listAcademicYears(null, PageRequest.of(0, 100));

		boolean found = tenantBYears.getContent().stream()
				.anyMatch(y -> tenantA.getId().equals(y.getTenantId()));
		assertFalse(found, "Tenant B must not see academic years created under tenant A");
	}

	@Test
	void currentYearFlag_isScopedPerTenant_notGlobal() {
		activateTenant(tenantA);
		academicYearService.create("2025-26", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true);

		activateTenant(tenantB);
		AcademicYear tenantBYear = academicYearService.create("2025-26",
				LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true);

		assertNotNull(tenantBYear, "Tenant B creating its own current year must not be blocked "
				+ "by tenant A's current year of the same name");
	}

	@Test
	void academicYearPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		AcademicYear year = academicYearService.create("2025-26",
				LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), false);
		String publicId = year.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> academicYearService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's academic year");
	}
}
