package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.FeeAssignmentService;
import com.altafjava.school.application.service.FeeDiscountService;
import com.altafjava.school.application.service.FeeInstallmentService;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.FeeRefundService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.fee.model.DiscountType;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeRefund;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.model.InstallmentStatus.State;
import com.altafjava.school.domain.fee.model.RefundStatus;
import com.altafjava.school.domain.student.model.Student;

/**
 * Discounts, installments and refunds against the real database: the balance a student sees
 * reflects all three, and two simultaneous refunds of one payment can never refund more than was
 * paid (the row lock, not luck).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class FeeAdjustmentsIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private StudentService studentService;
	@Autowired
	private FeeStructureService feeStructureService;
	@Autowired
	private FeeAssignmentService feeAssignmentService;
	@Autowired
	private FeePaymentService feePaymentService;
	@Autowired
	private FeeDiscountService feeDiscountService;
	@Autowired
	private FeeInstallmentService feeInstallmentService;
	@Autowired
	private FeeRefundService feeRefundService;
	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenant;
	private Student student;
	private FeeStructure structure;

	@BeforeEach
	void setUp() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand("Fees School", "fa-" + suffix, 1L,
				"admin@fa-" + suffix + ".test", "Password123!", "USD"));
		activate();
		student = studentService.enroll("STU-" + suffix, "Alice", "Smith", "alice@fa.test", LocalDate.of(2012, 1, 1));
		structure = feeStructureService.create("Tuition " + suffix, new BigDecimal("1000.00"), FeeFrequency.ANNUAL,
				"Standard");
		feeAssignmentService.assign(structure.getPublicId().toString(), student.getPublicId().toString(), null);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void activate() {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private FeeBalance balance() {
		return feePaymentService.calculateBalanceForStudent(tenant.getId(), student).get(0);
	}

	private FeePayment pay(String amount) {
		return feePaymentService.record(student.getPublicId().toString(), structure.getPublicId().toString(),
				new BigDecimal(amount),
				LocalDateTime.now(), "RCPT-" + UUID.randomUUID().toString().substring(0, 8));
	}

	private static void assertMoney(String expected, BigDecimal actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
	}

	@Test
	void aDiscountReducesWhatTheStudentOwes_andRevokingItRestoresIt() {
		var discount = feeDiscountService.grant(student.getPublicId().toString(), structure.getPublicId().toString(),
				DiscountType.PERCENTAGE, new BigDecimal("25"), "scholarship", "Merit scholarship", 1L);

		assertMoney("750.00", balance().amountDue());
		assertMoney("250.00", balance().discountAmount());

		feeDiscountService.revoke(student.getPublicId().toString(), discount.getPublicId().toString(), "Withdrawn");

		assertMoney("1000.00", balance().amountDue());
	}

	@Test
	void discountsCannotAddUpToMoreThanTheFee() {
		feeDiscountService.grant(student.getPublicId().toString(), structure.getPublicId().toString(),
				DiscountType.FIXED, new BigDecimal("900"), "STAFF_WARD", "Staff child", 1L);

		assertThrows(BusinessException.class,
				() -> feeDiscountService.grant(student.getPublicId().toString(), structure.getPublicId().toString(),
						DiscountType.PERCENTAGE, new BigDecimal("20"), "SIBLING", "Second child", 1L));
	}

	@Test
	void anInstallmentPlanSplitsTheNetFee_andPaymentsFillItOldestFirst() {
		feeDiscountService.grant(student.getPublicId().toString(), structure.getPublicId().toString(),
				DiscountType.FIXED, new BigDecimal("200"), "HARDSHIP", "Hardship", 1L);
		feeInstallmentService.setEvenPlan(student.getPublicId().toString(), structure.getPublicId().toString(), 4,
				LocalDate.now().plusDays(10), 1);
		pay("300.00");

		FeeBalance balance = balance();

		assertEquals(4, balance.installments().size());
		assertMoney("200.00", balance.installments().get(0).amount());
		assertEquals(State.PAID, balance.installments().get(0).state());
		assertMoney("100.00", balance.installments().get(1).paidAmount());
		assertEquals(State.PARTIALLY_PAID, balance.installments().get(1).state());
		assertMoney("500.00", balance.outstandingBalance());
	}

	@Test
	void replacingAPlanLeavesExactlyTheNewOneActive() {
		String studentId = student.getPublicId().toString();
		String structureId = structure.getPublicId().toString();
		feeInstallmentService.setEvenPlan(studentId, structureId, 4, LocalDate.now().plusDays(10), 1);

		feeInstallmentService.setEvenPlan(studentId, structureId, 2, LocalDate.now().plusDays(30), 3);

		assertEquals(2, feeInstallmentService.getPlan(studentId, structureId).size());
		assertEquals(2, balance().installments().size());
	}

	@Test
	void aRefundIsReflectedInTheBalance_andCannotExceedWhatWasPaid() {
		FeePayment payment = pay("1000.00");

		FeeRefund refund = feeRefundService.refund(payment.getPublicId().toString(), new BigDecimal("400.00"),
				"Billing error", 1L);

		assertEquals(RefundStatus.COMPLETED, refund.getStatus());
		assertTrue(refund.getCreditNoteNumber().startsWith("CN-"));
		assertMoney("600.00", balance().amountPaid());
		assertMoney("400.00", balance().outstandingBalance());
		assertThrows(BusinessException.class, () -> feeRefundService.refund(payment.getPublicId().toString(),
				new BigDecimal("700.00"), "Too much", 1L));
		assertEquals(1, feeRefundService.listForPayment(payment.getPublicId().toString()).size());
	}

	@Test
	void twoSimultaneousRefundsOfOnePaymentCannotRefundMoreThanWasPaid() throws Exception {
		FeePayment payment = pay("1000.00");
		String paymentId = payment.getPublicId().toString();
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		List<Callable<Boolean>> attempts = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			attempts.add(() -> {
				activate();
				start.await();
				try {
					feeRefundService.refund(paymentId, new BigDecimal("800.00"), "Race", 1L);
					return true;
				} catch (BusinessException e) {
					return false;
				} finally {
					TenantContext.ForTesting.clear();
				}
			});
		}
		List<Future<Boolean>> results = new ArrayList<>();
		for (Callable<Boolean> attempt : attempts) {
			results.add(pool.submit(attempt));
		}
		start.countDown();
		long succeeded = 0;
		for (Future<Boolean> result : results) {
			if (result.get()) {
				succeeded++;
			}
		}
		pool.shutdown();

		assertEquals(1, succeeded, "exactly one of two refunds of 800 against a 1000 payment may succeed");
		assertMoney("200.00", balance().amountPaid());
	}
}
