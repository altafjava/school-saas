package com.altafjava.school.application.alert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertRuleEvaluator;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

/**
 * {@code ruleType = "EXAM_SCHEDULE_REMINDER"}: notifies a student's guardian of exams scheduled
 * within the rule's threshold (days-before, default 2). Drives {@code ExamScheduleReminderJob}.
 */
@Component
public class ExamScheduleReminderRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "EXAM_SCHEDULE_REMINDER";
	private static final int DEFAULT_DAYS_BEFORE = 2;
	private static final String DATE_TIME_PATTERN = "EEE, MMM d 'at' HH:mm";

	private final ExamRepository examRepository;
	private final SubjectRepository subjectRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final TenantFormattingService tenantFormattingService;
	private final SchoolClock schoolClock;

	public ExamScheduleReminderRuleEvaluator(ExamRepository examRepository, SubjectRepository subjectRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository, StudentRepository studentRepository,
			StudentNotificationRecipientResolver recipientResolver, TenantFormattingService tenantFormattingService,
			SchoolClock schoolClock) {
		this.examRepository = examRepository;
		this.subjectRepository = subjectRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.studentRepository = studentRepository;
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
		int daysBefore = rule.getThresholdValue() != null ? rule.getThresholdValue().intValue()
				: DEFAULT_DAYS_BEFORE;
		LocalDateTime now = schoolClock.now(tenantId);

		List<AlertTrigger> triggers = new ArrayList<>();
		for (Exam exam : examRepository.findPublishedBetween(tenantId, ExamStatus.SCHEDULED, now,
				now.plusDays(daysBefore))) {
			triggers.addAll(triggersForExam(tenantId, exam));
		}
		return triggers;
	}

	private List<AlertTrigger> triggersForExam(Long tenantId, Exam exam) {
		Subject subject = subjectRepository.findByIdAndTenantId(exam.getSubjectId(), tenantId).orElse(null);
		if (subject == null) {
			return List.of();
		}
		List<Long> studentIds = studentClassroomLinkRepository.findAllByClassroomId(tenantId, exam.getClassroomId())
				.stream().map(StudentClassroomLink::getStudentId).distinct().toList();
		Map<Long, Student> studentsById = studentRepository.findAllByIdInAndTenantId(studentIds, tenantId).stream()
				.collect(Collectors.toMap(Student::getId, Function.identity()));

		List<AlertTrigger> triggers = new ArrayList<>();
		for (Long studentId : studentIds) {
			Student student = studentsById.get(studentId);
			if (student == null) {
				continue;
			}
			buildTrigger(tenantId, exam, subject, student).ifPresent(triggers::add);
		}
		return triggers;
	}

	private Optional<AlertTrigger> buildTrigger(Long tenantId, Exam exam, Subject subject, Student student) {
		DateTimeFormatter dateTimeFormat = tenantFormattingService.localizedFormatter(tenantId, DATE_TIME_PATTERN);
		return recipientResolver.resolve(tenantId, student)
				.map(userId -> {
					String studentName = student.getFirstName() + " " + student.getLastName();
					String scheduledAt = exam.getScheduledAt().format(dateTimeFormat);
					return new AlertTrigger(userId, "Upcoming Exam: " + exam.getTitle(),
							subject.getName() + " exam for " + studentName + " is scheduled for " + scheduledAt + ".",
							Map.of(
									"studentName", studentName,
									"examTitle", exam.getTitle(),
									"subjectName", subject.getName(),
									"scheduledAt", scheduledAt),
							NotificationPriority.NORMAL);
				});
	}
}
