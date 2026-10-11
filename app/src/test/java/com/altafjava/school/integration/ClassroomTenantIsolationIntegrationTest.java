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
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;

/**
 * Verifies that classroom records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class ClassroomTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private ClassroomService classroomService;

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
				"School A", "cls-a-" + suffix, 1L, "admin@cls-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "cls-b-" + suffix, 1L, "admin@cls-b.test", "Password123!", "USD"));
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

	private String createAcademicYear(String name) {
		var academicYear = academicYearService.create(name, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31),
				true);
		return academicYear.getPublicId().toString();
	}

	@Test
	void classroomCreatedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		String code = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		classroomService.create(code, "Grade 5", "A", createAcademicYear("2024-25"), null);

		activateTenant(tenantB);
		Page<Classroom> tenantBClassrooms = classroomService.searchClassrooms(PageRequest.of(0, 100), null, null, null);

		boolean found = tenantBClassrooms.getContent().stream()
				.anyMatch(c -> tenantA.getId().equals(c.getTenantId()));
		assertFalse(found, "Tenant B must not see classrooms created under tenant A");
	}

	@Test
	void classroomPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		String code = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		Classroom classroom = classroomService.create(code, "Grade 6", "B", createAcademicYear("2024-25"), null);
		String publicId = classroom.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> classroomService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's classroom");
	}
}
