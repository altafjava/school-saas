package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
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
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

	private static final UUID CLASSROOM_PUBLIC_ID = UUID.randomUUID();
	private static final UUID SUBJECT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private AssignmentRepository assignmentRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private AcademicAccessGuard academicAccessGuard;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;

	private AssignmentService assignmentService;

	@BeforeEach
	void setUp() {
		assignmentService = new AssignmentService(assignmentRepository, classroomRepository, subjectRepository,
				studentClassroomLinkRepository, studentRepository, academicAccessGuard,
				recipientResolver, notificationService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Classroom classroomWithId(long id) {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 10L, "2024-25", null);
		classroom.setId(id);
		return classroom;
	}

	private Subject subjectWithId(long id) {
		Subject subject = Subject.create("SUB-1", "Science", null);
		subject.setId(id);
		return subject;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private void stubClassroomAndSubject() {
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(classroomWithId(5L)));
		when(subjectRepository.findByPublicIdAndTenantId(SUBJECT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(subjectWithId(6L)));
		when(academicAccessGuard.requireTeacherOfSubject(1L, 5L, 6L)).thenReturn(7L);
	}

	@Test
	void create_withNonExistentClassroom_throwsResourceNotFound() {
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> assignmentService.create(CLASSROOM_PUBLIC_ID.toString(), SUBJECT_PUBLIC_ID.toString(), "Essay",
						"desc", null, LocalDate.now(), BigDecimal.TEN));

		verify(assignmentRepository, never()).save(any());
	}

	@Test
	void create_teacherNotScoped_throwsAccessDenied() {
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(classroomWithId(5L)));
		when(subjectRepository.findByPublicIdAndTenantId(SUBJECT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(subjectWithId(6L)));
		when(academicAccessGuard.requireTeacherOfSubject(1L, 5L, 6L))
				.thenThrow(new AccessDeniedException("not scoped"));

		assertThrows(AccessDeniedException.class,
				() -> assignmentService.create(CLASSROOM_PUBLIC_ID.toString(), SUBJECT_PUBLIC_ID.toString(), "Essay",
						"desc", null, LocalDate.now(), BigDecimal.TEN));

		verify(assignmentRepository, never()).save(any());
	}

	@Test
	void create_succeeds_andNotifiesResolvedRecipients() {
		stubClassroomAndSubject();
		when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
		StudentClassroomLink link = StudentClassroomLink.create(20L, 5L, 10L, LocalDate.now());
		when(studentClassroomLinkRepository.findByClassroomId(1L, 5L, PageRequest.of(0, 1000)))
				.thenReturn(new PageImpl<>(java.util.List.of(link)));
		Student student = studentWithId(20L);
		when(studentRepository.findAllByIdInAndTenantId(java.util.List.of(20L), 1L))
				.thenReturn(java.util.List.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(99L));
		LocalDate dueDate = LocalDate.now().plusDays(7);

		Assignment assignment = assertDoesNotThrow(() -> assignmentService.create(CLASSROOM_PUBLIC_ID.toString(),
				SUBJECT_PUBLIC_ID.toString(), "Essay", "desc", null, dueDate, BigDecimal.TEN));

		assertEquals(5L, assignment.getClassroomId());
		ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
		verify(notificationService, times(1)).send(captor.capture());
		SendNotificationCommand sent = captor.getValue();
		assertEquals("Alice Smith", sent.getTemplateVariables().get("studentName"));
		assertEquals("Essay", sent.getTemplateVariables().get("assignmentTitle"));
		assertEquals(dueDate.toString(), sent.getTemplateVariables().get("dueDate"));
		assertTrue(sent.getMessage().contains("Essay"));
	}

	@Test
	void create_studentWithNoLinkableRecipient_skipsSilentlyWithoutFailing() {
		stubClassroomAndSubject();
		when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
		StudentClassroomLink link = StudentClassroomLink.create(20L, 5L, 10L, LocalDate.now());
		when(studentClassroomLinkRepository.findByClassroomId(1L, 5L, PageRequest.of(0, 1000)))
				.thenReturn(new PageImpl<>(java.util.List.of(link)));
		Student student = studentWithId(20L);
		when(studentRepository.findAllByIdInAndTenantId(java.util.List.of(20L), 1L))
				.thenReturn(java.util.List.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> assignmentService.create(CLASSROOM_PUBLIC_ID.toString(),
				SUBJECT_PUBLIC_ID.toString(), "Essay", "desc", null, LocalDate.now().plusDays(7), BigDecimal.TEN));

		verify(notificationService, never()).send(any());
	}

	@Test
	void listByClassroom_delegatesVisibilityCheck() {
		when(classroomRepository.findByPublicIdAndTenantId(CLASSROOM_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(classroomWithId(5L)));
		Page<Assignment> expected = Page.empty();
		when(assignmentRepository.findByClassroomIdAndTenantId(5L, 1L, PageRequest.of(0, 20))).thenReturn(expected);

		Page<Assignment> result = assignmentService.listByClassroom(CLASSROOM_PUBLIC_ID.toString(),
				PageRequest.of(0, 20));

		verify(academicAccessGuard).assertCanViewCoursework(1L, 5L);
		assertEquals(expected, result);
	}

	@Test
	void reschedule_withValidTeacher_updatesDueDate() {
		UUID assignmentPublicId = UUID.randomUUID();
		Assignment assignment = Assignment.create(5L, 6L, 7L, "Essay", "desc", null, LocalDate.now(), BigDecimal.TEN);
		when(assignmentRepository.findByPublicIdAndTenantId(assignmentPublicId, 1L))
				.thenReturn(Optional.of(assignment));
		when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
		LocalDate newDueDate = LocalDate.now().plusDays(3);

		Assignment result = assignmentService.reschedule(assignmentPublicId.toString(), newDueDate);

		assertEquals(newDueDate, result.getDueDate());
		verify(academicAccessGuard).assertCanWriteSubject(1L, 5L, 6L);
	}

	@Test
	void reschedule_teacherOfAnotherSubject_throwsAccessDenied() {
		UUID assignmentPublicId = UUID.randomUUID();
		Assignment assignment = Assignment.create(5L, 6L, 7L, "Essay", "desc", null, LocalDate.now(), BigDecimal.TEN);
		when(assignmentRepository.findByPublicIdAndTenantId(assignmentPublicId, 1L))
				.thenReturn(Optional.of(assignment));
		doThrow(new AccessDeniedException("not scoped")).when(academicAccessGuard).assertCanWriteSubject(1L, 5L, 6L);

		assertThrows(AccessDeniedException.class,
				() -> assignmentService.reschedule(assignmentPublicId.toString(), LocalDate.now().plusDays(3)));

		verify(assignmentRepository, never()).save(any());
	}

	@Test
	void reschedule_withNonExistentAssignment_throwsResourceNotFound() {
		UUID assignmentPublicId = UUID.randomUUID();
		when(assignmentRepository.findByPublicIdAndTenantId(assignmentPublicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> assignmentService.reschedule(assignmentPublicId.toString(), LocalDate.now()));
	}
}
