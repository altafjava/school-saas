package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that students created under tenant A are not visible to tenant B.
 *
 * Phase 5 core validation: multi-tenant data isolation for school domain entities.
 * Creates real tenants via the platform's TenantOnboardingService so FK constraints are satisfied.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class StudentTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private StudentService studentService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "school-a-" + suffix, 1L, "admin@school-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "school-b-" + suffix, 1L, "admin@school-b.test", "Password123!", "USD"));
		TenantContext.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(), tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.clear();
	}

	@Test
	void studentEnrolledUnderTenantA_isNotVisibleToTenantB() {
		// Given — enroll a student under tenant A
		activateTenant(tenantA);
		String studentCode = "STU-" + UUID.randomUUID().toString().substring(0, 8);
		Student enrolled = studentService.enroll(
				studentCode, "Alice", "Smith", "alice@a.edu", LocalDate.of(2010, 5, 15));
		UUID publicId = enrolled.getPublicId();

		// When — switch to tenant B and list students
		activateTenant(tenantB);
		Page<Student> tenantBStudents = studentService.listStudents(PageRequest.of(0, 100));

		// Then — tenant B must not see tenant A's student
		boolean found = tenantBStudents.getContent().stream()
				.anyMatch(s -> s.getPublicId().equals(publicId));

		assertFalse(found, "Tenant B must not see students enrolled under tenant A");
	}

	@Test
	void studentPublicId_notAccessibleAcrossTenants() {
		// Given — enroll under tenant A
		activateTenant(tenantA);
		Student enrolled = studentService.enroll(
				"STU-" + UUID.randomUUID().toString().substring(0, 8),
				"Bob", "Jones", "bob@a.edu", LocalDate.of(2011, 3, 20));
		String publicId = enrolled.getPublicId().toString();

		// When/Then — tenant B cannot fetch tenant A's student by publicId
		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> studentService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's student");
	}

	@Test
	void eachTenant_seesOnlyItsOwnEnrolledStudents() {
		// Enroll one student in tenant A, one in tenant B
		activateTenant(tenantA);
		studentService.enroll("STU-A-" + UUID.randomUUID().toString().substring(0, 6),
				"Carol", "White", "carol@a.edu", LocalDate.of(2009, 7, 1));

		activateTenant(tenantB);
		studentService.enroll("STU-B-" + UUID.randomUUID().toString().substring(0, 6),
				"Dave", "Brown", "dave@b.edu", LocalDate.of(2010, 2, 14));

		// Tenant A sees only its own students
		activateTenant(tenantA);
		Page<Student> studentsA = studentService.listStudents(PageRequest.of(0, 100));
		assertTrue(studentsA.getContent().stream().allMatch(s -> tenantA.getId().equals(s.getTenantId())),
				"All students listed under tenant A must belong to tenant A");

		// Tenant B sees only its own students
		activateTenant(tenantB);
		Page<Student> studentsB = studentService.listStudents(PageRequest.of(0, 100));
		assertTrue(studentsB.getContent().stream().allMatch(s -> tenantB.getId().equals(s.getTenantId())),
				"All students listed under tenant B must belong to tenant B");
	}
}
