package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ReportCardService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.reportcard.model.ReportCard;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.term.model.Term;

/**
 * Verifies that report cards generated under tenant A are not visible to tenant B. Report cards
 * are always viewed through {@code StudentDataAccessGuard} (there is no tenant-wide list, only
 * "for this student"), so these tests authenticate as TENANT_ADMIN to exercise the real guard
 * bypass path, the same way an actual admin request would.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
class ReportCardTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private ReportCardService reportCardService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private TermService termService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "rpc-a-" + suffix, 1L, "admin@rpc-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "rpc-b-" + suffix, 1L, "admin@rpc-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
		authenticateAsTenantAdmin();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private void authenticateAsTenantAdmin() {
		AuthenticatedUser principal = new AuthenticatedUser() {
			@Override
			public Long getId() {
				return -1L;
			}

			@Override
			public String getUsername() {
				return "admin";
			}

			@Override
			public Long getTenantId() {
				return null;
			}
		};
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	@Test
	void reportCardGeneratedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice",
				"Smith", "alice@rpc.test", LocalDate.of(2010, 1, 1));
		// A fresh tenant already gets a default "current year" academic year seeded by
		// SchoolTenantProvisioningListener — use a distinct name here to avoid colliding with it.
		AcademicYear year = academicYearService.create("AY-" + UUID.randomUUID().toString().substring(0, 6),
				LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(5), true);
		Term term = termService.create("Term 1", LocalDate.now().minusDays(10), LocalDate.now().plusDays(10),
				year.getPublicId().toString());
		reportCardService.generate(student.getId(), term.getId(), null, null);
		String studentPublicId = student.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> reportCardService.listForStudent(studentPublicId, PageRequest.of(0, 20)),
				"Tenant B must not resolve tenant A's student at all");
	}

	@Test
	void reportCardPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Bob",
				"Jones", "bob@rpc.test", LocalDate.of(2011, 2, 2));
		// A fresh tenant already gets a default "current year" academic year seeded by
		// SchoolTenantProvisioningListener — use a distinct name here to avoid colliding with it.
		AcademicYear year = academicYearService.create("AY-" + UUID.randomUUID().toString().substring(0, 6),
				LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(5), true);
		Term term = termService.create("Term 1", LocalDate.now().minusDays(10), LocalDate.now().plusDays(10),
				year.getPublicId().toString());
		ReportCard reportCard = reportCardService.generate(student.getId(), term.getId(), null, null);
		String reportCardPublicId = reportCard.getPublicId().toString();
		String studentPublicId = student.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> reportCardService.findByPublicId(studentPublicId, reportCardPublicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's report card");
	}
}
