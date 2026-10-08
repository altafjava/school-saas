package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.application.service.AdmissionSettingsService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.admission.model.ApplicationFeeStatus;

/**
 * The application fee gates review and approval, approval issues a verifiable offer letter, and
 * the fee a school sets never leaks to or from another school.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
class AdmissionApplicationFeeIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private AdmissionService admissionService;
	@Autowired
	private AdmissionSettingsService admissionSettingsService;
	@Autowired
	private DocumentIssuanceService documentIssuanceService;
	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void setUp() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand("Fee School A", "af-a-" + suffix, 1L,
				"admin@af-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand("Fee School B", "af-b-" + suffix, 1L,
				"admin@af-b.test", "Password123!", "USD"));
		activate(tenantA);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void activate(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private Admission apply() {
		return admissionService.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith",
				"bob@family.test", "+14155552671", "Grade 3");
	}

	private String studentCode() {
		return "STU-" + UUID.randomUUID().toString().substring(0, 8);
	}

	@Test
	void withoutAConfiguredFee_applicationsAreNeverBlocked() {
		Admission admission = apply();

		assertEquals(ApplicationFeeStatus.NOT_REQUIRED, admission.getApplicationFeeStatus());
		assertEquals(AdmissionStatus.UNDER_REVIEW,
				admissionService.markUnderReview(admission.getPublicId().toString()).getStatus());
	}

	@Test
	void aPendingFeeBlocksReview_untilItIsPaidWithAReceipt() {
		admissionSettingsService.setApplicationFee(new BigDecimal("300.00"));
		Admission admission = apply();
		String publicId = admission.getPublicId().toString();

		assertEquals(ApplicationFeeStatus.PENDING, admission.getApplicationFeeStatus());
		assertThrows(BusinessException.class, () -> admissionService.markUnderReview(publicId));

		Admission paid = admissionService.recordApplicationFeePayment(publicId);

		assertEquals(ApplicationFeeStatus.PAID, paid.getApplicationFeeStatus());
		assertTrue(paid.getApplicationFeeReceiptNumber().startsWith("RCPT-"));
		assertEquals(AdmissionStatus.UNDER_REVIEW, admissionService.markUnderReview(publicId).getStatus());
	}

	@Test
	void aWaivedFeeAlsoClearsTheApplication_andTheAmountStaysFrozenWhenTheSchoolChangesItsFee() {
		admissionSettingsService.setApplicationFee(new BigDecimal("300.00"));
		Admission admission = apply();
		admissionSettingsService.setApplicationFee(new BigDecimal("500.00"));

		Admission waived = admissionService.waiveApplicationFee(admission.getPublicId().toString(), "Staff ward");

		assertEquals(ApplicationFeeStatus.WAIVED, waived.getApplicationFeeStatus());
		assertEquals(0, new BigDecimal("300.00").compareTo(waived.getApplicationFeeAmount()));
		assertEquals(AdmissionStatus.UNDER_REVIEW,
				admissionService.markUnderReview(admission.getPublicId().toString()).getStatus());
	}

	@Test
	void approvalIssuesAVerifiableOfferLetterPdf() {
		Admission admission = apply();

		Admission enrolled = admissionService.finalizeApproval(admission.getPublicId().toString(), "admin", "ok",
				studentCode());

		assertNotNull(enrolled.getOfferLetterIssuanceId(), "approval should have issued an offer letter");
		byte[] pdf = admissionService.downloadOfferLetter(admission.getPublicId().toString());
		assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
		var issuance = documentIssuanceService.findById(enrolled.getOfferLetterIssuanceId()).orElseThrow();
		assertEquals("ADMISSION_OFFER_LETTER", issuance.getDocumentType());
		assertEquals(issuance.getVerificationCode(),
				documentIssuanceService.verify(tenantA.getId(), issuance.getVerificationCode()).getVerificationCode());
	}

	@Test
	void reissuingTheOfferLetterRevokesTheOldOne() {
		Admission admission = apply();
		Admission enrolled = admissionService.finalizeApproval(admission.getPublicId().toString(), "admin", "ok",
				studentCode());
		Long firstLetter = enrolled.getOfferLetterIssuanceId();

		Admission reissued = admissionService.issueOfferLetter(admission.getPublicId().toString());

		assertTrue(!firstLetter.equals(reissued.getOfferLetterIssuanceId()));
		assertTrue(documentIssuanceService.findById(firstLetter).orElseThrow().isRevoked());
		assertTrue(!documentIssuanceService.findById(reissued.getOfferLetterIssuanceId()).orElseThrow().isRevoked());
	}

	@Test
	void theApplicationFeeIsPerSchool() {
		admissionSettingsService.setApplicationFee(new BigDecimal("300.00"));

		activate(tenantB);

		assertTrue(admissionSettingsService.getApplicationFee().isEmpty());
		assertNull(apply().getApplicationFeeAmount());
	}
}
