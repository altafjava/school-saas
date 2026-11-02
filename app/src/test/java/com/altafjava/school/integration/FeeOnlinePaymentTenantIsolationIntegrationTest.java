package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.exception.TechnicalException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.paymentgateway.service.PaymentChargeResult;
import com.altafjava.platform.domain.paymentgateway.service.PaymentWebhookEvent;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.FeeAssignmentService;
import com.altafjava.school.application.service.FeeOnlinePaymentService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentGatewayConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies FeeOnlinePaymentService's gateway-sourced fee payments respect tenant isolation, both
 * for the self-service create/confirm flow and for the webhook-driven recording path — matching
 * FeePaymentTenantIsolationIntegrationTest's shape for the pre-existing manual-record flow.
 */
@Import({ TestRedisConfig.class, TestPaymentGatewayConfig.class })
class FeeOnlinePaymentTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private FeeOnlinePaymentService feeOnlinePaymentService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private FeeStructureService feeStructureService;

	@Autowired
	private FeeAssignmentService feeAssignmentService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "fop-a-" + suffix, 1L, "admin@fop-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "fop-b-" + suffix, 1L, "admin@fop-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
		authenticateAsTenantAdmin();
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

	// createCharge/confirmCharge route through StudentDataAccessGuard, which requires a real
	// authenticated principal — TENANT_ADMIN bypasses the guard's ownership check.
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

	private Student createStudentWithOutstandingBalance(String suffix, String publicIdOut[]) {
		Student student = studentService.enroll("STU-" + suffix, "Alice", "Smith", "alice-" + suffix + "@a.edu",
				LocalDate.of(2010, 1, 1));
		var feeStructure = feeStructureService.create("Tuition " + suffix, BigDecimal.valueOf(500),
				FeeFrequency.MONTHLY, "Standard");
		feeAssignmentService.assign(feeStructure.getPublicId().toString(), student.getPublicId().toString(), null);
		publicIdOut[0] = feeStructure.getPublicId().toString();
		return student;
	}

	@Test
	void feePaymentConfirmedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String[] feeStructurePublicId = new String[1];
		Student student = createStudentWithOutstandingBalance(suffix, feeStructurePublicId);

		PaymentChargeResult charge = feeOnlinePaymentService.createCharge(student.getPublicId().toString(),
				feeStructurePublicId[0]);
		FeePayment payment = feeOnlinePaymentService.confirmCharge(student.getPublicId().toString(),
				feeStructurePublicId[0], charge.gatewayChargeReference());
		String publicId = payment.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> feeOnlinePaymentService.findReceiptForSelfService(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's gateway fee payment");
	}

	@Test
	void createCharge_gatewayDown_failsAndLeavesTheBalanceOwed() {
		activateTenant(tenantA);
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String[] feeStructurePublicId = new String[1];
		Student student = createStudentWithOutstandingBalance(suffix, feeStructurePublicId);
		TestPaymentGatewayConfig.GATEWAY_DOWN.set(true);
		try {
			assertThrows(TechnicalException.class, () -> feeOnlinePaymentService
					.createCharge(student.getPublicId().toString(), feeStructurePublicId[0]));
		} finally {
			TestPaymentGatewayConfig.GATEWAY_DOWN.set(false);
		}

		PaymentChargeResult retried = feeOnlinePaymentService.createCharge(student.getPublicId().toString(),
				feeStructurePublicId[0]);
		org.junit.jupiter.api.Assertions.assertNotNull(retried.gatewayChargeReference());
	}

	@Test
	void webhookEvent_referencingTenantAsMetadata_cannotBeRecordedUnderTenantB() {
		activateTenant(tenantA);
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String[] feeStructurePublicId = new String[1];
		Student student = createStudentWithOutstandingBalance(suffix, feeStructurePublicId);
		PaymentChargeResult charge = feeOnlinePaymentService.createCharge(student.getPublicId().toString(),
				feeStructurePublicId[0]);

		// Same gateway charge reference and metadata are replayed under tenant B's context — the
		// student/fee-structure lookups are tenant-scoped, so tenant B must not be able to record it.
		activateTenant(tenantB);
		PaymentWebhookEvent event = new PaymentWebhookEvent("payment_intent.succeeded",
				charge.gatewayChargeReference(), "succeeded",
				Map.of("studentPublicId", student.getPublicId().toString(), "feeStructurePublicId",
						feeStructurePublicId[0]));

		assertThrows(ResourceNotFoundException.class, () -> feeOnlinePaymentService.recordFromWebhookEvent(event),
				"Tenant B must not be able to record a webhook event referencing tenant A's student");
	}

	@Test
	void confirmCharge_isIdempotent_secondConfirmReturnsSamePayment() {
		activateTenant(tenantA);
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String[] feeStructurePublicId = new String[1];
		Student student = createStudentWithOutstandingBalance(suffix, feeStructurePublicId);
		PaymentChargeResult charge = feeOnlinePaymentService.createCharge(student.getPublicId().toString(),
				feeStructurePublicId[0]);

		FeePayment first = feeOnlinePaymentService.confirmCharge(student.getPublicId().toString(),
				feeStructurePublicId[0], charge.gatewayChargeReference());
		FeePayment second = feeOnlinePaymentService.confirmCharge(student.getPublicId().toString(),
				feeStructurePublicId[0], charge.gatewayChargeReference());

		assertEquals(first.getId(), second.getId());
		assertEquals(0, BigDecimal.valueOf(500).compareTo(first.getPaidAmount()));
	}
}
