package com.altafjava.school.application.alert;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertRuleEvaluator;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * {@code ruleType = "FEE_PAYMENT_REMINDER"}: reminds a student's guardian of an unpaid fee or
 * installment the rule's threshold of days before it falls due (default 3), and again on the day.
 * Overdue amounts belong to {@link FeeOverdueReminderRuleEvaluator}.
 */
@Component
public class FeePaymentReminderRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "FEE_PAYMENT_REMINDER";
	static final int DEFAULT_DAYS_BEFORE = 3;
	static final String DATE_PATTERN = "d MMM yyyy";

	private final StudentRepository studentRepository;
	private final FeePaymentService feePaymentService;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final TenantFormattingService tenantFormattingService;
	private final SchoolClock schoolClock;

	public FeePaymentReminderRuleEvaluator(StudentRepository studentRepository, FeePaymentService feePaymentService,
			StudentNotificationRecipientResolver recipientResolver, TenantFormattingService tenantFormattingService,
			SchoolClock schoolClock) {
		this.studentRepository = studentRepository;
		this.feePaymentService = feePaymentService;
		this.recipientResolver = recipientResolver;
		this.tenantFormattingService = tenantFormattingService;
		this.schoolClock = schoolClock;
	}

	@Override
	public String supportedRuleType() {
		return RULE_TYPE;
	}

	@Override
	public List<AlertTrigger> evaluate(AlertRule rule) {
		Long tenantId = rule.getTenantId();
		LocalDate today = schoolClock.today(tenantId);
		LocalDate ahead = today.plusDays(rule.getThresholdValue() != null
				? Math.max(0, rule.getThresholdValue().intValue())
				: DEFAULT_DAYS_BEFORE);
		DateTimeFormatter dateFormat = tenantFormattingService.localizedFormatter(tenantId, DATE_PATTERN);
		List<Student> students = studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE,
				tenantId);
		Map<Long, List<FeeBalance>> balances = feePaymentService.calculateBalancesForStudents(tenantId, students);

		List<AlertTrigger> triggers = new ArrayList<>();
		for (Student student : students) {
			List<FeeDueItem> due = FeeDueItem.unpaid(balances.getOrDefault(student.getId(), List.of())).stream()
					.filter(FeeDueItem.dueOn(ahead).or(FeeDueItem.dueOn(today))).toList();
			if (!due.isEmpty()) {
				recipientResolver.resolve(tenantId, student)
						.map(userId -> trigger(userId, student, due, dateFormat))
						.ifPresent(triggers::add);
			}
		}
		return triggers;
	}

	private static AlertTrigger trigger(Long userId, Student student, List<FeeDueItem> due,
			DateTimeFormatter dateFormat) {
		String studentName = student.getFirstName() + " " + student.getLastName();
		String amount = FeeDueItem.total(due).toPlainString();
		String dueDate = FeeDueItem.earliest(due).format(dateFormat);
		return new AlertTrigger(userId, "Fee Payment Reminder",
				"A fee payment of " + amount + " for " + studentName + " is due on " + dueDate + ".",
				Map.of("studentName", studentName, "amount", amount, "dueDate", dueDate),
				NotificationPriority.NORMAL);
	}
}
