package com.altafjava.school.application.alert;

import static com.altafjava.school.application.alert.FeePaymentReminderRuleEvaluatorTest.CLOCK;
import static com.altafjava.school.application.alert.FeePaymentReminderRuleEvaluatorTest.TODAY;
import static com.altafjava.school.application.alert.FeePaymentReminderRuleEvaluatorTest.balance;
import static com.altafjava.school.application.alert.FeePaymentReminderRuleEvaluatorTest.studentWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
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
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeOverdueReminderRuleEvaluatorTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeePaymentService feePaymentService;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private TenantFormattingService tenantFormattingService;

	private FeeOverdueReminderRuleEvaluator evaluator;

	@BeforeEach
	void setUp() {
		evaluator = new FeeOverdueReminderRuleEvaluator(studentRepository, feePaymentService, recipientResolver,
				tenantFormattingService, TestSchoolClocks.fixed(CLOCK, ZoneOffset.UTC));
		when(tenantFormattingService.localizedFormatter(eq(1L), any()))
				.thenReturn(DateTimeFormatter.ofPattern(FeePaymentReminderRuleEvaluator.DATE_PATTERN, Locale.US));
	}

	private static AlertRule rule(Integer repeatDays) {
		AlertRule rule = AlertRule.create(FeeOverdueReminderRuleEvaluator.RULE_TYPE, "Fee overdue", true,
				repeatDays == null ? null : BigDecimal.valueOf(repeatDays), SchoolNotificationTypes.FEE_OVERDUE,
				null);
		rule.setTenantId(1L);
		return rule;
	}

	private void balances(Student student, FeeBalance... balances) {
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalancesForStudents(1L, List.of(student)))
				.thenReturn(Map.of(student.getId(), List.of(balances)));
	}

	@Test
	void evaluate_firstDayOverdue_remindsWithEverythingOverdue() {
		Student student = studentWithId(1L);
		balances(student, balance(400, TODAY.minusDays(1)), balance(600, TODAY.plusDays(5)));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		List<AlertTrigger> triggers = evaluator.evaluate(rule(7));

		assertEquals(1, triggers.size());
		AlertTrigger trigger = triggers.getFirst();
		assertEquals("400", trigger.templateVariables().get("amount"));
		assertEquals("9 Apr 2026", trigger.templateVariables().get("dueDate"));
		assertEquals("1", trigger.templateVariables().get("daysOverdue"));
		assertEquals(NotificationPriority.HIGH, trigger.priority());
	}

	@Test
	void evaluate_repeatsOnTheCadenceFromTheOldestOverdueDate() {
		Student student = studentWithId(1L);
		balances(student, balance(400, TODAY.minusDays(8)), balance(300, TODAY.minusDays(3)));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		List<AlertTrigger> triggers = evaluator.evaluate(rule(7));

		assertEquals(1, triggers.size());
		assertEquals("700", triggers.getFirst().templateVariables().get("amount"));
		assertEquals("8", triggers.getFirst().templateVariables().get("daysOverdue"));
	}

	@Test
	void evaluate_betweenReminders_staysQuiet() {
		balances(studentWithId(1L), balance(400, TODAY.minusDays(4)));

		assertTrue(evaluator.evaluate(rule(7)).isEmpty());
	}

	@Test
	void evaluate_onTheDueDate_isNotYetOverdue() {
		balances(studentWithId(1L), balance(400, TODAY));

		assertTrue(evaluator.evaluate(rule(7)).isEmpty());
	}

	@Test
	void evaluate_withNoThreshold_usesTheDefaultCadence() {
		Student student = studentWithId(1L);
		balances(student, balance(400, TODAY.minusDays(1 + FeeOverdueReminderRuleEvaluator.DEFAULT_REPEAT_DAYS)));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		assertEquals(1, evaluator.evaluate(rule(null)).size());
	}

	@Test
	void evaluate_paidFee_staysQuiet() {
		balances(studentWithId(1L), balance(0, TODAY.minusDays(1)));

		assertTrue(evaluator.evaluate(rule(7)).isEmpty());
	}
}
