package com.altafjava.school.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeePaymentReminderRuleEvaluatorTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeePaymentService feePaymentService;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;

	private FeePaymentReminderRuleEvaluator evaluator;

	private void newEvaluator() {
		evaluator = new FeePaymentReminderRuleEvaluator(studentRepository, feePaymentService, recipientResolver);
	}

	private AlertRule rule() {
		AlertRule rule = AlertRule.create(FeePaymentReminderRuleEvaluator.RULE_TYPE, "Fee reminder", true, null,
				SchoolNotificationTypes.FEE_DUE, null);
		rule.setTenantId(1L);
		return rule;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void evaluate_studentWithOutstandingBalance_returnsTrigger() {
		newEvaluator();
		Student student = studentWithId(1L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalancesForStudents(1L, List.of(student)))
				.thenReturn(Map.of(1L, List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(1000),
						BigDecimal.valueOf(400), BigDecimal.valueOf(600), BigDecimal.ZERO, BigDecimal.ZERO, null))));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		List<AlertTrigger> triggers = evaluator.evaluate(rule());

		assertEquals(1, triggers.size());
		AlertTrigger trigger = triggers.get(0);
		assertEquals(42L, trigger.recipientUserId());
		assertTrue(trigger.message().contains("600"));
		assertEquals("600", trigger.templateVariables().get("amount"));
	}

	@Test
	void evaluate_studentWithNoOutstandingBalance_returnsNoTriggers() {
		newEvaluator();
		Student student = studentWithId(1L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalancesForStudents(1L, List.of(student)))
				.thenReturn(Map.of(1L, List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(1000),
						BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null))));

		assertTrue(evaluator.evaluate(rule()).isEmpty());
	}

	@Test
	void evaluate_outstandingBalanceButNoResolvedRecipient_returnsNoTriggers() {
		newEvaluator();
		Student student = studentWithId(1L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalancesForStudents(1L, List.of(student)))
				.thenReturn(Map.of(1L, List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(1000),
						BigDecimal.ZERO, BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO, null))));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.empty());

		assertTrue(evaluator.evaluate(rule()).isEmpty());
	}

	@Test
	void evaluate_multipleStudentsWithBalance_returnsOneTriggerEach() {
		newEvaluator();
		Student studentA = studentWithId(1L);
		Student studentB = studentWithId(2L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(studentA, studentB));
		when(feePaymentService.calculateBalancesForStudents(1L, List.of(studentA, studentB)))
				.thenReturn(Map.of(
						1L, List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(500), BigDecimal.ZERO,
								BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.ZERO, null)),
						2L, List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(500), BigDecimal.ZERO,
								BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.ZERO, null))));
		when(recipientResolver.resolve(1L, studentA)).thenReturn(Optional.of(41L));
		when(recipientResolver.resolve(1L, studentB)).thenReturn(Optional.of(42L));

		assertEquals(2, evaluator.evaluate(rule()).size());
	}
}
