package com.altafjava.school.application.alert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertRuleEvaluator;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * {@code ruleType = "LIBRARY_BOOK_OVERDUE"}: reminds a student's guardian of a circulation overdue
 * by more than the rule's threshold (grace days, default 0 — today's immediate-on-overdue
 * behavior). Drives {@code LibraryOverdueReminderJob}.
 */
@Component
public class LibraryOverdueRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "LIBRARY_BOOK_OVERDUE";
	private static final int DEFAULT_GRACE_DAYS = 0;

	private final CirculationRepository circulationRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final SchoolClock schoolClock;

	public LibraryOverdueRuleEvaluator(CirculationRepository circulationRepository,
			StudentRepository studentRepository, StudentNotificationRecipientResolver recipientResolver,
			SchoolClock schoolClock) {
		this.circulationRepository = circulationRepository;
		this.studentRepository = studentRepository;
		this.recipientResolver = recipientResolver;
		this.schoolClock = schoolClock;
	}

	@Override
	public String supportedRuleType() {
		return RULE_TYPE;
	}

	@Override
	public List<AlertTrigger> evaluate(AlertRule rule) {
		Long tenantId = rule.getTenantId();
		int graceDays = rule.getThresholdValue() != null ? rule.getThresholdValue().intValue() : DEFAULT_GRACE_DAYS;
		LocalDate today = schoolClock.today(tenantId);

		List<Circulation> overdueCirculations = circulationRepository.findAllByTenantIdAndReturnedAtIsNull(tenantId)
				.stream()
				.filter(circulation -> today.isAfter(circulation.getDueDate().plusDays(graceDays)))
				.toList();
		if (overdueCirculations.isEmpty()) {
			return List.of();
		}

		List<Long> studentIds = overdueCirculations.stream()
				.map(Circulation::getStudentId)
				.distinct()
				.toList();
		Map<Long, Student> studentsById = studentRepository.findAllByIdInAndTenantId(studentIds, tenantId).stream()
				.collect(Collectors.toMap(Student::getId, Function.identity()));

		List<AlertTrigger> triggers = new ArrayList<>();
		for (Circulation circulation : overdueCirculations) {
			Student student = studentsById.get(circulation.getStudentId());
			if (student != null) {
				buildTrigger(tenantId, circulation, student).ifPresent(triggers::add);
			}
		}
		return triggers;
	}

	private Optional<AlertTrigger> buildTrigger(Long tenantId, Circulation circulation, Student student) {
		return recipientResolver.resolve(tenantId, student)
				.map(userId -> {
					String studentName = student.getFirstName() + " " + student.getLastName();
					return new AlertTrigger(userId, "Overdue Library Book",
							"A library book is overdue since " + circulation.getDueDate(),
							Map.of(
									"studentName", studentName,
									"dueDate", circulation.getDueDate().toString()),
							NotificationPriority.NORMAL);
				});
	}
}
