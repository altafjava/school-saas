package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.CertificateService;
import com.altafjava.school.application.service.CertificateTypeService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.certificate.model.CertificateType;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that a certificate (and its verification code) issued under tenant A is not visible or
 * verifiable from tenant B — including through the public-facing verification lookup, which is the
 * one surface a caller from outside the issuing tenant is most likely to reach.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
class CertificateTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private CertificateService certificateService;

	@Autowired
	private CertificateTypeService certificateTypeService;

	@Autowired
	private DocumentIssuanceService documentIssuanceService;

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
				"Cert School A", "cert-a-" + suffix, 1L, "admin@cert-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Cert School B", "cert-b-" + suffix, 1L, "admin@cert-b.test", "Password123!", "USD"));
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
	void certificateIssuedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice",
				"Smith", "alice@cert.test", LocalDate.of(2010, 1, 1));
		CertificateType template = certificateTypeService.create("BONAFIDE", "Bonafide Certificate",
				"This certifies {{studentName}}.");
		certificateService.issue(student.getPublicId().toString(), template.getPublicId().toString(), 1L);
		String studentPublicId = student.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> certificateService.listForStudent(studentPublicId, PageRequest.of(0, 20)),
				"Tenant B must not resolve tenant A's student at all");
	}

	@Test
	void certificateVerificationCode_notVerifiableFromAnotherTenant() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Bob",
				"Jones", "bob@cert.test", LocalDate.of(2011, 2, 2));
		CertificateType template = certificateTypeService.create("TRANSFER", "Transfer Certificate",
				"This certifies {{studentName}}.");
		DocumentIssuance issuance = certificateService.issue(student.getPublicId().toString(),
				template.getPublicId().toString(), 1L);
		String verificationCode = issuance.getVerificationCode();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> documentIssuanceService.verify(tenantB.getId(), verificationCode),
				"Tenant B must not verify a certificate issued under tenant A, even with the correct code");
	}
}
