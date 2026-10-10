package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.altafjava.school.application.filter.FeePaymentFilter;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that fee payment records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class FeePaymentTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private FeePaymentService feePaymentService;

	@Autowired
	private StudentService studentService;

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
				"School A", "fp-a-" + suffix, 1L, "admin@fp-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "fp-b-" + suffix, 1L, "admin@fp-b.test", "Password123!", "USD"));
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
	void feePaymentRecordedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6),
				"Alice", "Smith", "alice@a.edu", LocalDate.of(2010, 1, 1));
		FeeStructure structure = feeStructureService.create(
				"Tuition " + UUID.randomUUID().toString().substring(0, 6),
				BigDecimal.valueOf(500), FeeFrequency.MONTHLY, "Standard");
		feePaymentService.record(student.getPublicId().toString(), structure.getPublicId().toString(),
				BigDecimal.valueOf(500),
				LocalDateTime.now(), "RCPT-" + UUID.randomUUID().toString().substring(0, 8));

		activateTenant(tenantB);
		Page<FeePayment> tenantBPayments = feePaymentService.listFeePayments(FeePaymentFilter.NONE,
				PageRequest.of(0, 100));

		boolean found = tenantBPayments.getContent().stream()
				.anyMatch(p -> tenantA.getId().equals(p.getTenantId()));
		assertFalse(found, "Tenant B must not see fee payments recorded under tenant A");
	}

	@Test
	void feePaymentPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6),
				"Bob", "Jones", "bob@a.edu", LocalDate.of(2011, 3, 20));
		FeeStructure structure = feeStructureService.create(
				"Tuition " + UUID.randomUUID().toString().substring(0, 6),
				BigDecimal.valueOf(500), FeeFrequency.MONTHLY, "Standard");
		FeePayment payment = feePaymentService.record(student.getPublicId().toString(),
				structure.getPublicId().toString(),
				BigDecimal.valueOf(500), LocalDateTime.now(),
				"RCPT-" + UUID.randomUUID().toString().substring(0, 8));
		String publicId = payment.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> feePaymentService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's fee payment");
	}
}
