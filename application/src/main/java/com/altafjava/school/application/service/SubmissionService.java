package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.lms.model.Assignment;
import com.altafjava.school.domain.lms.model.Submission;
import com.altafjava.school.domain.lms.repository.AssignmentRepository;
import com.altafjava.school.domain.lms.repository.SubmissionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class SubmissionService {

	private final SubmissionRepository submissionRepository;
	private final AssignmentRepository assignmentRepository;
	private final StudentRepository studentRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final AcademicAccessGuard academicAccessGuard;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;

	public SubmissionService(SubmissionRepository submissionRepository, AssignmentRepository assignmentRepository,
			StudentRepository studentRepository, StudentClassroomLinkRepository studentClassroomLinkRepository,
			AcademicAccessGuard academicAccessGuard,
			StudentNotificationRecipientResolver recipientResolver, NotificationService notificationService) {
		this.submissionRepository = submissionRepository;
		this.assignmentRepository = assignmentRepository;
		this.studentRepository = studentRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.academicAccessGuard = academicAccessGuard;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
	}

	@Transactional
	public Submission submit(String assignmentPublicId, String storageKey, String textContent) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Assignment assignment = assignmentRepository
				.findByPublicIdAndTenantId(UUID.fromString(assignmentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentPublicId));
		Student student = resolveCurrentStudent(tenantId);
		if (studentClassroomLinkRepository
				.findByStudentIdAndClassroomId(tenantId, student.getId(), assignment.getClassroomId())
				.isEmpty()) {
			throw new ResourceNotFoundException(
					"Student " + student.getId() + " is not enrolled in classroom " + assignment.getClassroomId());
		}
		if (submissionRepository.existsByAssignmentIdAndStudentIdAndTenantId(assignment.getId(), student.getId(),
				tenantId)) {
			throw new BusinessException(
					"A submission already exists for student " + student.getId() + " on assignment "
							+ assignmentPublicId);
		}
		boolean isLate = LocalDateTime.now().toLocalDate().isAfter(assignment.getDueDate());
		Submission submission = Submission.submit(assignment.getId(), student.getId(), storageKey, textContent,
				isLate);
		return submissionRepository.save(submission);
	}

	@Transactional(readOnly = true)
	public Page<Submission> list(String assignmentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Assignment assignment = assignmentRepository
				.findByPublicIdAndTenantId(UUID.fromString(assignmentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentPublicId));
		academicAccessGuard.assertCanReadSubject(tenantId, assignment.getClassroomId(), assignment.getSubjectId());
		return submissionRepository.findByAssignmentIdAndTenantId(assignment.getId(), tenantId, pageable);
	}

	@Transactional
	public Submission grade(String assignmentPublicId, String submissionPublicId, BigDecimal marks,
			String feedback) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Assignment assignment = assignmentRepository
				.findByPublicIdAndTenantId(UUID.fromString(assignmentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentPublicId));
		Submission submission = submissionRepository
				.findByPublicIdAndTenantId(UUID.fromString(submissionPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionPublicId));
		if (!submission.getAssignmentId().equals(assignment.getId())) {
			throw new ResourceNotFoundException("Submission not found: " + submissionPublicId);
		}
		Long gradingTeacherId = academicAccessGuard.requireTeacherOfSubject(tenantId, assignment.getClassroomId(),
				assignment.getSubjectId());
		submission.grade(marks, feedback, gradingTeacherId);
		Submission saved = submissionRepository.save(submission);
		notifyStudent(tenantId, saved, assignment);
		return saved;
	}

	private Student resolveCurrentStudent(Long tenantId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return studentRepository.findByUserIdAndTenantId(user.getId(), tenantId)
					.orElseThrow(() -> new AccessDeniedException("No student record linked to the current user"));
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot resolve submitting student");
	}

	private void notifyStudent(Long tenantId, Submission submission, Assignment assignment) {
		studentRepository.findByIdAndTenantId(submission.getStudentId(), tenantId)
				.ifPresent(student -> recipientResolver.resolve(tenantId, student)
						.ifPresent(userId -> notificationService.send(SendNotificationCommand.builder()
								.tenantId(tenantId)
								.userId(userId)
								.type(NotificationType.SUBMISSION_GRADED)
								.title("Submission Graded")
								.message("Your submission has been graded: " + submission.getMarksObtained()
										+ " marks.")
								.templateVariables(Map.of(
										"assignmentTitle", assignment.getTitle(),
										"marksObtained", String.valueOf(submission.getMarksObtained()),
										"feedback", submission.getFeedback() != null ? submission.getFeedback() : ""))
								.priority(NotificationPriority.NORMAL)
								.build())));
	}
}
