package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.GuardianConsentService;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.GuardianConsentType;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.student.model.Student;

/**
 * A guardian's consent record (data-processing consent for their linked minor student) must never
 * be readable, or grantable/revocable, from another tenant — even given the same guardian public
 * id/student public id values. Required per CLAUDE.md: "every new multi-tenant feature requires a
 * cross-tenant isolation test before merge."
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class GuardianConsentTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	private static final Long GUARDIAN_USER_ID = 77L;

	@Autowired
	private GuardianConsentService guardianConsentService;

	@Autowired
	private GuardianService guardianService;

	@Autowired
	private GuardianRepository guardianRepository;

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
				"Consent School A", "consent-a-" + suffix, 1L, "admin@consent-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Consent School B", "consent-b-" + suffix, 1L, "admin@consent-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	// The account behind the guardian is a bare user id here, so it is set on the record directly.
	private void linkToUser(Guardian guardian) {
		guardian.setUserId(GUARDIAN_USER_ID);
		guardianRepository.save(guardian);
	}

	@Test
	void consentGrantedUnderTenantA_notReachableFromTenantB() {
		activateTenant(tenantA);
		authenticateAsUser(GUARDIAN_USER_ID);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice",
				"Smith", "alice@consent.test", LocalDate.of(2012, 1, 1));
		Guardian guardian = guardianService.create("Bob", "Smith", "bob@consent.test", "+14155552671",
				null);
		linkToUser(guardian);
		guardianService.linkToStudent(guardian.getPublicId().toString(), student.getPublicId().toString(),
				RelationshipType.MOTHER, true);
		guardianConsentService.grant(student.getPublicId().toString(), GuardianConsentType.DATA_PROCESSING,
				"2026-01");
		String studentPublicId = student.getPublicId().toString();

		activateTenant(tenantB);
		authenticateAsUser(GUARDIAN_USER_ID);
		assertThrows(ResourceNotFoundException.class,
				() -> guardianConsentService.grant(studentPublicId, GuardianConsentType.DATA_PROCESSING, "2026-01"),
				"Tenant B must not resolve tenant A's student at all");
		assertThrows(ResourceNotFoundException.class,
				() -> guardianConsentService.listForStudent(studentPublicId),
				"Tenant B's admin view must not resolve tenant A's student either");
	}

	@Test
	void listForStudent_returnsOnlyThatTenantsOwnConsentRecords() {
		activateTenant(tenantA);
		authenticateAsUser(GUARDIAN_USER_ID);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Carol",
				"Jones", "carol@consent.test", LocalDate.of(2013, 3, 3));
		Guardian guardian = guardianService.create("Dave", "Jones", "dave@consent.test", "+14155552672",
				null);
		linkToUser(guardian);
		guardianService.linkToStudent(guardian.getPublicId().toString(), student.getPublicId().toString(),
				RelationshipType.MOTHER, true);
		guardianConsentService.grant(student.getPublicId().toString(), GuardianConsentType.DATA_PROCESSING,
				"2026-01");

		assertEquals(1, guardianConsentService.listForStudent(student.getPublicId().toString()).size());
	}
}
