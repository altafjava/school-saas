package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.lms.model.Assignment;
import com.altafjava.school.domain.lms.model.Submission;
import com.altafjava.school.domain.lms.model.SubmissionStatus;
import com.altafjava.school.domain.lms.repository.AssignmentRepository;
import com.altafjava.school.domain.lms.repository.SubmissionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

	private static final UUID ASSIGNMENT_PUBLIC_ID = UUID.randomUUID();
	private static final Long CURRENT_USER_ID = 55L;

	@Mock
	private SubmissionRepository submissionRepository;
	@Mock
	private AssignmentRepository assignmentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private AcademicAccessGuard academicAccessGuard;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;

	private SubmissionService submissionService;

	@BeforeEach
	void setUp() {
		submissionService = new SubmissionService(submissionRepository, assignmentRepository, studentRepository,
				studentClassroomLinkRepository, academicAccessGuard, recipientResolver,
				notificationService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	private Assignment assignmentWithId(long id, Long classroomId, LocalDate dueDate) {
		Assignment assignment = Assignment.create(classroomId, 6L, 7L, "Essay", "desc", null, dueDate,
				BigDecimal.TEN);
		assignment.setId(id);
		return assignment;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void submit_withNoLinkedStudentRecord_throwsAccessDenied() {
		authenticateAsUser(CURRENT_USER_ID);
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now().plusDays(5));
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		when(studentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(AccessDeniedException.class,
				() -> submissionService.submit(ASSIGNMENT_PUBLIC_ID.toString(), null, "My answer"));

		verify(submissionRepository, never()).save(any());
	}

	@Test
	void submit_studentNotEnrolledInAssignmentClassroom_throwsResourceNotFound() {
		authenticateAsUser(CURRENT_USER_ID);
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now().plusDays(5));
		Student student = studentWithId(20L);
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		when(studentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 20L, 5L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> submissionService.submit(ASSIGNMENT_PUBLIC_ID.toString(), null, "My answer"));

		verify(submissionRepository, never()).save(any());
	}

	@Test
	void submit_duplicateForSameStudentAndAssignment_throwsBusinessException() {
		authenticateAsUser(CURRENT_USER_ID);
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now().plusDays(5));
		Student student = studentWithId(20L);
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		when(studentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 20L, 5L))
				.thenReturn(Optional.of(StudentClassroomLink.create(20L, 5L, 10L, LocalDate.now())));
		when(submissionRepository.existsByAssignmentIdAndStudentIdAndTenantId(3L, 20L, 1L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> submissionService.submit(ASSIGNMENT_PUBLIC_ID.toString(), null, "My answer"));

		verify(submissionRepository, never()).save(any());
	}

	@Test
	void submit_beforeDueDate_isMarkedSubmitted() {
		authenticateAsUser(CURRENT_USER_ID);
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now().plusDays(5));
		Student student = studentWithId(20L);
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		when(studentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 20L, 5L))
				.thenReturn(Optional.of(StudentClassroomLink.create(20L, 5L, 10L, LocalDate.now())));
		when(submissionRepository.existsByAssignmentIdAndStudentIdAndTenantId(3L, 20L, 1L)).thenReturn(false);
		when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

		Submission submission = assertDoesNotThrow(
				() -> submissionService.submit(ASSIGNMENT_PUBLIC_ID.toString(), null, "My answer"));

		assertEquals(SubmissionStatus.SUBMITTED, submission.getStatus());
	}

	@Test
	void submit_afterDueDate_isMarkedLate() {
		authenticateAsUser(CURRENT_USER_ID);
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now().minusDays(2));
		Student student = studentWithId(20L);
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		when(studentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 20L, 5L))
				.thenReturn(Optional.of(StudentClassroomLink.create(20L, 5L, 10L, LocalDate.now())));
		when(submissionRepository.existsByAssignmentIdAndStudentIdAndTenantId(3L, 20L, 1L)).thenReturn(false);
		when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

		Submission submission = assertDoesNotThrow(
				() -> submissionService.submit(ASSIGNMENT_PUBLIC_ID.toString(), null, "My answer"));

		assertEquals(SubmissionStatus.LATE, submission.getStatus());
	}

	@Test
	void list_returnsSubmissionsForAssignment() {
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now());
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		Page<Submission> expected = Page.empty();
		when(submissionRepository.findByAssignmentIdAndTenantId(3L, 1L, PageRequest.of(0, 20)))
				.thenReturn(expected);

		Page<Submission> result = submissionService.list(ASSIGNMENT_PUBLIC_ID.toString(), PageRequest.of(0, 20));

		assertEquals(expected, result);
		verify(academicAccessGuard).assertCanReadSubject(1L, 5L, 6L);
	}

	@Test
	void list_teacherOfAnotherSubject_throwsAccessDenied() {
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now());
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		doThrow(new AccessDeniedException("not scoped")).when(academicAccessGuard).assertCanReadSubject(1L, 5L, 6L);

		assertThrows(AccessDeniedException.class,
				() -> submissionService.list(ASSIGNMENT_PUBLIC_ID.toString(), PageRequest.of(0, 20)));

		verify(submissionRepository, never()).findByAssignmentIdAndTenantId(any(), any(), any());
	}

	@Test
	void grade_teacherScoped_gradesAndNotifiesStudent() {
		UUID submissionPublicId = UUID.randomUUID();
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now());
		Submission submission = Submission.submit(3L, 20L, null, "My answer", false);
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		when(submissionRepository.findByPublicIdAndTenantId(submissionPublicId, 1L))
				.thenReturn(Optional.of(submission));
		when(academicAccessGuard.requireTeacherOfSubject(1L, 5L, 6L)).thenReturn(9L);
		when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
		Student student = studentWithId(20L);
		when(studentRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(99L));

		Submission graded = assertDoesNotThrow(() -> submissionService.grade(ASSIGNMENT_PUBLIC_ID.toString(),
				submissionPublicId.toString(), BigDecimal.valueOf(90), "Nice work"));

		assertEquals(SubmissionStatus.GRADED, graded.getStatus());
		assertEquals(9L, graded.getGradedBy());
		verify(notificationService, times(1)).send(any(SendNotificationCommand.class));
	}

	@Test
	void grade_teacherNotScoped_throwsAccessDenied() {
		UUID submissionPublicId = UUID.randomUUID();
		Assignment assignment = assignmentWithId(3L, 5L, LocalDate.now());
		Submission submission = Submission.submit(3L, 20L, null, "My answer", false);
		when(assignmentRepository.findByPublicIdAndTenantId(ASSIGNMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(assignment));
		when(submissionRepository.findByPublicIdAndTenantId(submissionPublicId, 1L))
				.thenReturn(Optional.of(submission));
		when(academicAccessGuard.requireTeacherOfSubject(1L, 5L, 6L))
				.thenThrow(new AccessDeniedException("not scoped"));

		assertThrows(AccessDeniedException.class, () -> submissionService.grade(ASSIGNMENT_PUBLIC_ID.toString(),
				submissionPublicId.toString(), BigDecimal.valueOf(90), "Nice work"));

		verify(submissionRepository, never()).save(any());
	}
}
