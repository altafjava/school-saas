package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
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
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;

/**
 * Verifies that fee structure records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class FeeStructureTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private FeeStructureService feeStructureService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "fst-a-" + suffix, 1L, "admin@fst-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "fst-b-" + suffix, 1L, "admin@fst-b.test", "Password123!", "USD"));
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
	void feeStructureCreatedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		feeStructureService.create("Tuition " + UUID.randomUUID().toString().substring(0, 6),
				BigDecimal.valueOf(500), FeeFrequency.MONTHLY, "Standard");

		activateTenant(tenantB);
		Page<FeeStructure> tenantBStructures = feeStructureService.listFeeStructures(null, PageRequest.of(0, 100));

		boolean found = tenantBStructures.getContent().stream()
				.anyMatch(fs -> tenantA.getId().equals(fs.getTenantId()));
		assertFalse(found, "Tenant B must not see fee structures created under tenant A");
	}

	@Test
	void feeStructurePublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		FeeStructure structure = feeStructureService.create(
				"Tuition " + UUID.randomUUID().toString().substring(0, 6),
				BigDecimal.valueOf(500), FeeFrequency.MONTHLY, "Standard");
		String publicId = structure.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> feeStructureService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's fee structure");
	}
}
