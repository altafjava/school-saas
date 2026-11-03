package com.altafjava.school.application.listener;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.exam.event.ExamSchedulePublishedEvent;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sends each enrolled student's family the classroom's newly published exam dates. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamSchedulePublishedListener {

	private static final String DATE_TIME_PATTERN = "EEE, MMM d 'at' HH:mm";

	private final ExamRepository examRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;
	private final TenantFormattingService tenantFormattingService;

	@Async("platformTaskExecutor")
	@EventListener
	public void onExamSchedulePublished(ExamSchedulePublishedEvent event) {
		List<Exam> exams = examRepository.findAllByIdInAndTenantId(event.examIds(), event.tenantId()).stream()
				.sorted(Comparator.comparing(Exam::getScheduledAt)).toList();
		if (exams.isEmpty()) {
			return;
		}
		String schedule = describe(event.tenantId(), exams);
		List<Long> studentIds = studentClassroomLinkRepository
				.findAllByClassroomId(event.tenantId(), event.classroomId()).stream()
				.map(StudentClassroomLink::getStudentId).distinct().toList();
		int notified = 0;
		for (Student student : studentRepository.findAllByIdInAndTenantId(studentIds, event.tenantId())) {
			notified += notify(event.tenantId(), student, schedule);
		}
		log.info("action=exam-schedule-published-notified tenantId={} classroomId={} exams={} notified={}",
				event.tenantId(), event.classroomId(), exams.size(), notified);
	}

	private String describe(Long tenantId, List<Exam> exams) {
		DateTimeFormatter format = tenantFormattingService.localizedFormatter(tenantId, DATE_TIME_PATTERN);
		return exams.stream().map(exam -> exam.getTitle() + ": " + exam.getScheduledAt().format(format))
				.collect(Collectors.joining("; "));
	}

	private int notify(Long tenantId, Student student, String schedule) {
		return recipientResolver.resolve(tenantId, student).map(userId -> {
			notificationService.send(SendNotificationCommand.builder()
					.tenantId(tenantId)
					.userId(userId)
					.type(SchoolNotificationTypes.EXAM_SCHEDULED)
					.title("Exam timetable published")
					.message("Exam dates for " + student.getFirstName() + " " + student.getLastName() + ": " + schedule
							+ ".")
					.templateVariables(Map.of("schedule", schedule))
					.priority(NotificationPriority.NORMAL)
					.build());
			return 1;
		}).orElse(0);
	}
}
