package com.altafjava.school.application.listener;

import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.exam.event.ExamResultsPublishedEvent;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Tells each graded student's family that an exam's results are now visible. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamResultsPublishedListener {

	private final ExamRepository examRepository;
	private final GradeRepository gradeRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;

	@Async("platformTaskExecutor")
	@EventListener
	public void onExamResultsPublished(ExamResultsPublishedEvent event) {
		Exam exam = examRepository.findByIdAndTenantId(event.examId(), event.tenantId()).orElse(null);
		if (exam == null || !exam.isResultsPublished()) {
			return;
		}
		List<Long> studentIds = gradeRepository.findAllByExamIdAndTenantId(exam.getId(), event.tenantId()).stream()
				.map(Grade::getStudentId)
				.distinct()
				.toList();
		int notified = 0;
		for (Student student : studentRepository.findAllByIdInAndTenantId(studentIds, event.tenantId())) {
			notified += notify(event.tenantId(), exam, student);
		}
		log.info("action=exam-results-published-notified tenantId={} examId={} notified={}", event.tenantId(),
				exam.getId(), notified);
	}

	private int notify(Long tenantId, Exam exam, Student student) {
		return recipientResolver.resolve(tenantId, student).map(userId -> {
			notificationService.send(SendNotificationCommand.builder()
					.tenantId(tenantId)
					.userId(userId)
					.type(NotificationType.GRADE_PUBLISHED)
					.title("Results published: " + exam.getTitle())
					.message("The results of " + exam.getTitle() + " are now available for "
							+ student.getFirstName() + " " + student.getLastName() + ".")
					.templateVariables(Map.of("examTitle", exam.getTitle()))
					.priority(NotificationPriority.NORMAL)
					.build());
			return 1;
		}).orElse(0);
	}
}
