package com.altafjava.school.application.privacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.altafjava.school.application.messaging.CommunicationDataEraser;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.repository.AnnouncementRepository;
import com.altafjava.school.domain.messaging.model.Conversation;
import com.altafjava.school.domain.messaging.repository.ConversationRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class SchoolDataRetentionHandlerTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private AnnouncementRepository announcementRepository;
	@Mock
	private ConversationRepository conversationRepository;
	@Mock
	private CommunicationDataEraser communicationDataEraser;
	@Mock
	private PlatformTransactionManager transactionManager;

	private SchoolDataRetentionHandler handler;

	@BeforeEach
	void setUp() {
		lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
		handler = new SchoolDataRetentionHandler(studentRepository, announcementRepository, conversationRepository,
				communicationDataEraser, transactionManager);
	}

	private Student withdrawnStudent(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		student.withdraw();
		return student;
	}

	@Test
	void enforceRetention_entityTypeNotStudent_returnsZeroWithoutQuerying() {
		int affected = handler.enforceRetention(1L, "GUARDIAN", Instant.now(), "ANONYMIZE");

		assertEquals(0, affected);
		verify(studentRepository, never())
				.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(any(), any(), any());
	}

	@Test
	void enforceRetention_anonymizePolicy_erasesPiiAndSoftDeletesEachEligibleStudent() {
		Student student = withdrawnStudent(10L);
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				any(), eq(cutoff))).thenReturn(List.of(student));
		when(studentRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(student));

		int affected = handler.enforceRetention(1L, "STUDENT", cutoff, "ANONYMIZE");

		assertEquals(1, affected);
		assertEquals("[erased]", student.getFirstName());
		assertEquals(true, student.isDeleted());
		verify(studentRepository, times(1)).save(student);
	}

	@Test
	void enforceRetention_softDeletePolicy_softDeletesWithoutErasingPii() {
		Student student = withdrawnStudent(11L);
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				any(), eq(cutoff))).thenReturn(List.of(student));
		when(studentRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(student));

		handler.enforceRetention(1L, "STUDENT", cutoff, "SOFT_DELETE");

		assertEquals("Alice", student.getFirstName());
		assertEquals(true, student.isDeleted());
	}

	@Test
	void enforceRetention_hardDeletePolicy_throwsAfterAttemptingEveryEligibleStudent() {
		Student student = withdrawnStudent(12L);
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				any(), eq(cutoff))).thenReturn(List.of(student));
		when(studentRepository.findByIdAndTenantId(12L, 1L)).thenReturn(Optional.of(student));

		assertThrows(IllegalStateException.class,
				() -> handler.enforceRetention(1L, "STUDENT", cutoff, "HARD_DELETE"));
		verify(studentRepository, never()).save(any());
	}

	@Test
	void enforceRetention_oneStudentFailsAnotherSucceeds_stillCommitsTheHealthyOne() {
		Student failingStudent = withdrawnStudent(20L);
		Student healthyStudent = withdrawnStudent(21L);
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				any(), eq(cutoff))).thenReturn(List.of(failingStudent, healthyStudent));
		when(studentRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.empty());
		when(studentRepository.findByIdAndTenantId(21L, 1L)).thenReturn(Optional.of(healthyStudent));

		assertThrows(IllegalStateException.class,
				() -> handler.enforceRetention(1L, "STUDENT", cutoff, "ANONYMIZE"));

		assertEquals("[erased]", healthyStudent.getFirstName());
		verify(studentRepository, times(1)).save(healthyStudent);
	}

	@Test
	void enforceRetention_queriesOnlyInactiveEnrollmentStatuses() {
		Instant cutoff = Instant.now();
		when(studentRepository.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(eq(1L),
				eq(List.of(EnrollmentStatus.WITHDRAWN, EnrollmentStatus.GRADUATED, EnrollmentStatus.TRANSFERRED)),
				eq(cutoff))).thenReturn(List.of());

		handler.enforceRetention(1L, "STUDENT", cutoff, "ANONYMIZE");

		verify(studentRepository).findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(1L,
				List.of(EnrollmentStatus.WITHDRAWN, EnrollmentStatus.GRADUATED, EnrollmentStatus.TRANSFERRED), cutoff);
	}

	@Test
	void enforceRetention_hardDeletesIdleConversations() {
		Conversation conversation = Conversation.open(1L, 2L, 3L, 4L, 5L);
		when(conversationRepository.findIdleSince(9L, java.time.Instant.EPOCH))
				.thenReturn(java.util.List.of(conversation));

		int affected = handler.enforceRetention(9L, "CONVERSATION", java.time.Instant.EPOCH, "HARD_DELETE");

		org.junit.jupiter.api.Assertions.assertEquals(1, affected);
		verify(communicationDataEraser).deleteConversation(9L, conversation);
	}

	@Test
	void enforceRetention_erasesConversationBodiesWhenAnonymizing() {
		Conversation conversation = Conversation.open(1L, 2L, 3L, 4L, 5L);
		when(conversationRepository.findIdleSince(9L, java.time.Instant.EPOCH))
				.thenReturn(java.util.List.of(conversation));

		handler.enforceRetention(9L, "CONVERSATION", java.time.Instant.EPOCH, "ANONYMIZE");

		verify(communicationDataEraser).eraseConversation(9L, conversation);
	}

	@Test
	void enforceRetention_anonymizesClosedAnnouncements() {
		Announcement announcement = Announcement.builder().title("Notice").body("Text").build();
		when(announcementRepository.findClosedBefore(org.mockito.ArgumentMatchers.eq(9L), any(),
				org.mockito.ArgumentMatchers.eq(java.time.Instant.EPOCH))).thenReturn(java.util.List.of(announcement));

		handler.enforceRetention(9L, "ANNOUNCEMENT", java.time.Instant.EPOCH, "ANONYMIZE");

		org.junit.jupiter.api.Assertions.assertEquals("[erased]", announcement.getTitle());
		org.junit.jupiter.api.Assertions.assertTrue(announcement.isDeleted());
	}
}
