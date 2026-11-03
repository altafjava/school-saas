package com.altafjava.school.application.alert;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
 * {@code ruleType = "FEE_OVERDUE_REMINDER"}: tells a student's guardian what is overdue on the
 * first day past its due date, then every threshold days (default 7) until it is paid. The cadence
 * counts from the oldest overdue date, so a family gets one reminder per cycle, not one per fee.
 */
@Component
public class FeeOverdueReminderRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "FEE_OVERDUE_REMINDER";
	static final int DEFAULT_REPEAT_DAYS = 7;

	private final StudentRepository studentRepository;
	private final FeePaymentService feePaymentService;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final TenantFormattingService tenantFormattingService;
	private final SchoolClock schoolClock;

	public FeeOverdueReminderRuleEvaluator(StudentRepository studentRepository, FeePaymentService feePaymentService,
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
		int repeatDays = rule.getThresholdValue() != null ? Math.max(1, rule.getThresholdValue().intValue())
				: DEFAULT_REPEAT_DAYS;
		DateTimeFormatter dateFormat = tenantFormattingService.localizedFormatter(tenantId,
				FeePaymentReminderRuleEvaluator.DATE_PATTERN);
		List<Student> students = studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE,
				tenantId);
		Map<Long, List<FeeBalance>> balances = feePaymentService.calculateBalancesForStudents(tenantId, students);

		List<AlertTrigger> triggers = new ArrayList<>();
		for (Student student : students) {
			List<FeeDueItem> overdue = FeeDueItem.unpaid(balances.getOrDefault(student.getId(), List.of())).stream()
					.filter(item -> item.dueDate().isBefore(today)).toList();
			if (overdue.isEmpty()) {
				continue;
			}
			LocalDate since = FeeDueItem.earliest(overdue);
			long daysOverdue = ChronoUnit.DAYS.between(since, today);
			if ((daysOverdue - 1) % repeatDays == 0) {
				recipientResolver.resolve(tenantId, student)
						.map(userId -> trigger(userId, student, overdue, since.format(dateFormat), daysOverdue))
						.ifPresent(triggers::add);
			}
		}
		return triggers;
	}

	private static AlertTrigger trigger(Long userId, Student student, List<FeeDueItem> overdue, String since,
			long daysOverdue) {
		String studentName = student.getFirstName() + " " + student.getLastName();
		String amount = FeeDueItem.total(overdue).toPlainString();
		return new AlertTrigger(userId, "Fee Payment Overdue",
				"A fee payment of " + amount + " for " + studentName + " has been overdue since " + since + ".",
				Map.of("studentName", studentName, "amount", amount, "dueDate", since, "daysOverdue",
						Long.toString(daysOverdue)),
				NotificationPriority.HIGH);
	}
}
