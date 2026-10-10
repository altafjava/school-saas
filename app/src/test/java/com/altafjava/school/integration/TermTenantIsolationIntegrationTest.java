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
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.term.model.Term;

/**
 * Verifies that term records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class TermTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private TermService termService;

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
				"School A", "term-a-" + suffix, 1L, "admin@term-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "term-b-" + suffix, 1L, "admin@term-b.test", "Password123!", "USD"));
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
	void termCreatedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		AcademicYear year = academicYearService.create("2025-26",
				LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), false);
		termService.create("Term 1", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 30),
				year.getPublicId().toString());

		activateTenant(tenantB);
		Page<Term> tenantBTerms = termService.listTerms(PageRequest.of(0, 100));

		boolean found = tenantBTerms.getContent().stream()
				.anyMatch(t -> tenantA.getId().equals(t.getTenantId()));
		assertFalse(found, "Tenant B must not see terms created under tenant A");
	}

	@Test
	void termPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		AcademicYear year = academicYearService.create("2025-26",
				LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), false);
		Term term = termService.create("Term 1", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 30),
				year.getPublicId().toString());
		String publicId = term.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> termService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's term");
	}

	@Test
	void termReferencingAnotherTenantsAcademicYear_isRejected() {
		activateTenant(tenantA);
		AcademicYear yearA = academicYearService.create("2025-26",
				LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), false);

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> termService.create("Term 1", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 30),
						yearA.getPublicId().toString()),
				"Tenant B must not be able to create a term against tenant A's academic year");
	}
}
