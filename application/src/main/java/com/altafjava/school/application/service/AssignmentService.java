package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.lms.model.Assignment;
import com.altafjava.school.domain.lms.repository.AssignmentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@Service
public class AssignmentService {

	// Bounds the roster scan when broadcasting the "new assignment" notification — no school-saas
	// classroom roster is expected to approach this size.
	private static final int MAX_ROSTER_SCAN = 1000;

	private final AssignmentRepository assignmentRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentRepository studentRepository;
	private final AcademicAccessGuard academicAccessGuard;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;

	public AssignmentService(AssignmentRepository assignmentRepository, ClassroomRepository classroomRepository,
			SubjectRepository subjectRepository, StudentClassroomLinkRepository studentClassroomLinkRepository,
			StudentRepository studentRepository, AcademicAccessGuard academicAccessGuard,
			StudentNotificationRecipientResolver recipientResolver, NotificationService notificationService) {
		this.assignmentRepository = assignmentRepository;
		this.classroomRepository = classroomRepository;
		this.subjectRepository = subjectRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.studentRepository = studentRepository;
		this.academicAccessGuard = academicAccessGuard;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
	}

	@Transactional
	public Assignment create(String classroomPublicId, String subjectPublicId, String title, String description,
			String storageKey, LocalDate dueDate, BigDecimal maxMarks) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId));
		Long subjectId = subjectRepository.findByPublicIdAndTenantId(UUID.fromString(subjectPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectPublicId))
				.getId();
		Long teacherId = academicAccessGuard.requireTeacherOfSubject(tenantId, classroom.getId(), subjectId);
		Assignment assignment = Assignment.create(classroom.getId(), subjectId, teacherId, title, description,
				storageKey, dueDate, maxMarks);
		Assignment saved = assignmentRepository.save(assignment);
		notifyRoster(tenantId, classroom.getId(), saved);
		return saved;
	}

	@Transactional(readOnly = true)
	public Page<Assignment> listByClassroom(String classroomPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId));
		academicAccessGuard.assertCanViewCoursework(tenantId, classroom.getId());
		return assignmentRepository.findByClassroomIdAndTenantId(classroom.getId(), tenantId, pageable);
	}

	@Transactional
	public Assignment reschedule(String assignmentPublicId, LocalDate newDueDate, ExpectedVersion expectedVersion) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Assignment assignment = assignmentRepository
				.findByPublicIdAndTenantId(UUID.fromString(assignmentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentPublicId));
		expectedVersion.verify(assignment);
		academicAccessGuard.assertCanWriteSubject(tenantId, assignment.getClassroomId(), assignment.getSubjectId());
		assignment.reschedule(newDueDate);
		return assignmentRepository.save(assignment);
	}

	private void notifyRoster(Long tenantId, Long classroomId, Assignment assignment) {
		List<Long> studentIds = studentClassroomLinkRepository
				.findByClassroomId(tenantId, classroomId, PageRequest.of(0, MAX_ROSTER_SCAN))
				.getContent().stream()
				.map(StudentClassroomLink::getStudentId)
				.toList();
		for (Student student : studentRepository.findAllByIdInAndTenantId(studentIds, tenantId)) {
			notifyRecipient(tenantId, student, assignment);
		}
	}

	private void notifyRecipient(Long tenantId, Student student, Assignment assignment) {
		recipientResolver.resolve(tenantId, student).ifPresent(userId -> notificationService
				.send(SendNotificationCommand.builder()
						.tenantId(tenantId)
						.userId(userId)
						.type(NotificationType.ASSIGNMENT_POSTED)
						.title("New Assignment: " + assignment.getTitle())
						.message("A new assignment \"" + assignment.getTitle() + "\" has been posted, due "
								+ assignment.getDueDate() + ".")
						.templateVariables(Map.of(
								"studentName", student.getFirstName() + " " + student.getLastName(),
								"assignmentTitle", assignment.getTitle(),
								"dueDate", assignment.getDueDate().toString()))
						.priority(NotificationPriority.NORMAL)
						.build()));
	}
}
