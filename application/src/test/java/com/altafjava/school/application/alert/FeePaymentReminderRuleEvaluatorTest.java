package com.altafjava.school.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.InstallmentStatus;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeePaymentReminderRuleEvaluatorTest {

	static final LocalDate TODAY = LocalDate.of(2026, 4, 10);
	static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-10T06:00:00Z"), ZoneOffset.UTC);

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeePaymentService feePaymentService;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private TenantFormattingService tenantFormattingService;

	private FeePaymentReminderRuleEvaluator evaluator;

	@BeforeEach
	void setUp() {
		evaluator = new FeePaymentReminderRuleEvaluator(studentRepository, feePaymentService, recipientResolver,
				tenantFormattingService, TestSchoolClocks.fixed(CLOCK, ZoneOffset.UTC));
		when(tenantFormattingService.localizedFormatter(eq(1L), any()))
				.thenReturn(DateTimeFormatter.ofPattern(FeePaymentReminderRuleEvaluator.DATE_PATTERN, Locale.US));
	}

	private static AlertRule rule(Integer daysBefore) {
		AlertRule rule = AlertRule.create(FeePaymentReminderRuleEvaluator.RULE_TYPE, "Fee reminder", true,
				daysBefore == null ? null : BigDecimal.valueOf(daysBefore), SchoolNotificationTypes.FEE_DUE, null);
		rule.setTenantId(1L);
		return rule;
	}

	static Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	static FeeBalance balance(long outstanding, LocalDate dueDate) {
		return new FeeBalance(10L, "Tuition", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000 - outstanding),
				BigDecimal.valueOf(outstanding), BigDecimal.ZERO, BigDecimal.ZERO, dueDate);
	}

	private void balances(Student student, FeeBalance... balances) {
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalancesForStudents(1L, List.of(student)))
				.thenReturn(Map.of(student.getId(), List.of(balances)));
	}

	@Test
	void evaluate_feeDueInTheThresholdDays_remindsWithAmountAndDate() {
		Student student = studentWithId(1L);
		balances(student, balance(600, TODAY.plusDays(3)));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		List<AlertTrigger> triggers = evaluator.evaluate(rule(3));

		assertEquals(1, triggers.size());
		AlertTrigger trigger = triggers.getFirst();
		assertEquals(42L, trigger.recipientUserId());
		assertEquals("600", trigger.templateVariables().get("amount"));
		assertEquals("13 Apr 2026", trigger.templateVariables().get("dueDate"));
	}

	@Test
	void evaluate_feeDueToday_remindsAgain() {
		Student student = studentWithId(1L);
		balances(student, balance(600, TODAY));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		assertEquals(1, evaluator.evaluate(rule(3)).size());
	}

	@Test
	void evaluate_feeDueOnAnyOtherDay_staysQuiet() {
		Student student = studentWithId(1L);
		balances(student, balance(600, TODAY.plusDays(2)), balance(600, TODAY.plusDays(30)),
				balance(600, TODAY.minusDays(1)), balance(600, null));

		assertTrue(evaluator.evaluate(rule(3)).isEmpty());
	}

	@Test
	void evaluate_withNoThreshold_usesTheDefaultLeadTime() {
		Student student = studentWithId(1L);
		balances(student, balance(600, TODAY.plusDays(FeePaymentReminderRuleEvaluator.DEFAULT_DAYS_BEFORE)));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		assertEquals(1, evaluator.evaluate(rule(null)).size());
	}

	@Test
	void evaluate_paidFee_staysQuiet() {
		balances(studentWithId(1L), balance(0, TODAY));

		assertTrue(evaluator.evaluate(rule(3)).isEmpty());
	}

	@Test
	void evaluate_installmentPlan_remindsOnlyForTheInstallmentFallingDue() {
		Student student = studentWithId(1L);
		FeeBalance plan = new FeeBalance(10L, "Tuition", BigDecimal.valueOf(900), BigDecimal.ZERO,
				BigDecimal.valueOf(900), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(900), BigDecimal.ZERO,
				BigDecimal.ZERO, TODAY.plusDays(3), List.of(
						installment(1, TODAY.plusDays(3)), installment(2, TODAY.plusDays(33)),
						installment(3, TODAY.plusDays(63))));
		balances(student, plan);
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		List<AlertTrigger> triggers = evaluator.evaluate(rule(3));

		assertEquals(1, triggers.size());
		assertEquals("300", triggers.getFirst().templateVariables().get("amount"));
	}

	@Test
	void evaluate_noResolvedRecipient_staysQuiet() {
		Student student = studentWithId(1L);
		balances(student, balance(600, TODAY));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.empty());

		assertTrue(evaluator.evaluate(rule(3)).isEmpty());
	}

	static InstallmentStatus installment(int sequence, LocalDate dueDate) {
		return new InstallmentStatus(sequence, dueDate, BigDecimal.valueOf(300), BigDecimal.ZERO,
				BigDecimal.valueOf(300), InstallmentStatus.State.UPCOMING);
	}
}
