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
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.student.model.Student;

// Verifies guardian records/links created under tenant A are not visible to tenant B.
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class GuardianTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private GuardianService guardianService;

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
				"School A", "guardian-a-" + suffix, 1L, "admin@guardian-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "guardian-b-" + suffix, 1L, "admin@guardian-b.test", "Password123!", "USD"));
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
	void guardianCreatedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		guardianService.create("Jane", "Doe", "jane@guardian-a.test", "+14155552671", null);

		activateTenant(tenantB);
		Page<Guardian> tenantBGuardians = guardianService.searchGuardians(PageRequest.of(0, 100), null);

		boolean found = tenantBGuardians.getContent().stream()
				.anyMatch(g -> tenantA.getId().equals(g.getTenantId()));
		assertFalse(found, "Tenant B must not see guardians created under tenant A");
	}

	@Test
	void guardianPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		Guardian guardian = guardianService.create("Jane", "Doe", "jane@guardian-a.test", "+14155552671", null);
		String publicId = guardian.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> guardianService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's guardian");
	}

	@Test
	void linkToStudent_referencingAnotherTenantsStudent_isRejected() {
		activateTenant(tenantA);
		Student studentA = studentService.enroll("STU-GTI-1", "Alice", "Smith", "alice@guardian-a.test",
				LocalDate.of(2010, 1, 1));

		activateTenant(tenantB);
		Guardian guardianB = guardianService.create("Bob", "Jones", "bob@guardian-b.test", "+14155552672", null);

		assertThrows(ResourceNotFoundException.class,
				() -> guardianService.linkToStudent(guardianB.getPublicId().toString(),
						studentA.getPublicId().toString(), RelationshipType.FATHER, true),
				"Tenant B must not be able to link its guardian to tenant A's student");
	}

	@Test
	void studentGuardianLink_isolatedPerTenant() {
		activateTenant(tenantA);
		Student studentA = studentService.enroll("STU-GTI-2", "Alice", "Smith", "alice2@guardian-a.test",
				LocalDate.of(2010, 1, 1));
		Guardian guardianA = guardianService.create("Jane", "Doe", "jane2@guardian-a.test", "+14155552673", null);
		StudentGuardianLink link = guardianService.linkToStudent(guardianA.getPublicId().toString(),
				studentA.getPublicId().toString(), RelationshipType.MOTHER, true);

		activateTenant(tenantB);
		boolean crossTenantLinkExists = guardianService.searchGuardians(PageRequest.of(0, 100), null).getContent()
				.stream()
				.anyMatch(g -> g.getId().equals(link.getGuardianId()));
		assertFalse(crossTenantLinkExists, "Tenant B must not see tenant A's guardian via any listing");
	}
}
