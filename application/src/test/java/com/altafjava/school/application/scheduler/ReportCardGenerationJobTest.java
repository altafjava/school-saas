package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.application.service.ReportCardService;
import com.altafjava.school.domain.reportcard.repository.ReportCardRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class ReportCardGenerationJobTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TermRepository termRepository;
	@Mock
	private ReportCardService reportCardService;
	@Mock
	private TenantAdminNotifier tenantAdminNotifier;
	@Mock
	private ReportCardRepository reportCardRepository;

	private ReportCardGenerationJob job;

	@BeforeEach
	void setUp() {
		job = new ReportCardGenerationJob(studentRepository, termRepository, reportCardService, tenantAdminNotifier,
				reportCardRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		lenient().when(reportCardRepository.findStudentIdByTermIdAndTenantIdAndCreatedAtGreaterThanEqual(any(),
				any(), any())).thenReturn(List.of());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "ReportCardGeneration", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	private Term termWithId(long id, String name) {
		Term term = Term.create(name, LocalDate.now().minusDays(30), LocalDate.now().plusDays(30), 1L);
		term.setId(id);
		return term;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void execute_withCurrentTermAndActiveStudents_generatesReportCardsAndNotifiesAdmins() {
		Term term = termWithId(5L, "Term 1");
		Student student1 = studentWithId(1L);
		Student student2 = studentWithId(2L);
		when(termRepository.findCurrentByTenantId(eq(1L))).thenReturn(Optional.of(term));
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student1, student2));
		when(tenantAdminNotifier.notifyAll(eq(1L), any(), any())).thenReturn(1);

		JobExecutionResult result = job.execute(context());

		verify(reportCardService).generate(eq(1L), eq(5L), any(), any(), any());
		verify(reportCardService).generate(eq(2L), eq(5L), any(), any(), any());
		verify(tenantAdminNotifier).notifyAll(eq(1L), any(), org.mockito.ArgumentMatchers.contains("2"));
		assertEquals(new JobExecutionResult.Success(Map.of("generatedCount", 2, "notifiedCount", 1), null), result);
	}

	@Test
	void execute_withNoCurrentTerm_skipsWithoutError() {
		when(termRepository.findCurrentByTenantId(eq(1L))).thenReturn(Optional.empty());

		JobExecutionResult result = job.execute(context());

		verify(reportCardService, never()).generate(anyLong(), anyLong(), any(), any(), any());
		verify(tenantAdminNotifier, never()).notifyAll(any(), any(), any());
		assertEquals(new JobExecutionResult.Success(Map.of("generatedCount", 0, "notifiedCount", 0), null), result);
	}

	@Test
	void execute_withNoActiveStudents_doesNotNotify() {
		Term term = termWithId(5L, "Term 1");
		when(termRepository.findCurrentByTenantId(eq(1L))).thenReturn(Optional.of(term));
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of());

		JobExecutionResult result = job.execute(context());

		verify(tenantAdminNotifier, never()).notifyAll(any(), any(), any());
		assertEquals(new JobExecutionResult.Success(Map.of("generatedCount", 0, "notifiedCount", 0), null), result);
	}

	@Test
	void execute_whenOneStudentGenerationFails_continuesWithRemainingStudents() {
		Term term = termWithId(5L, "Term 1");
		Student student1 = studentWithId(1L);
		Student student2 = studentWithId(2L);
		when(termRepository.findCurrentByTenantId(eq(1L))).thenReturn(Optional.of(term));
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student1, student2));
		when(reportCardService.generate(eq(1L), eq(5L), any(), any(), any()))
				.thenThrow(new RuntimeException("storage unavailable"));
		when(tenantAdminNotifier.notifyAll(eq(1L), any(), any())).thenReturn(1);

		JobExecutionResult result = job.execute(context());

		verify(reportCardService).generate(eq(2L), eq(5L), any(), any(), any());
		assertEquals(new JobExecutionResult.Success(Map.of("generatedCount", 1, "notifiedCount", 1), null), result);
	}

	@Test
	void execute_whenRetryFindsStudentAlreadyGeneratedInThisRun_skipsRegeneratingThatStudent() {
		Term term = termWithId(5L, "Term 1");
		Student student1 = studentWithId(1L);
		Student student2 = studentWithId(2L);
		when(termRepository.findCurrentByTenantId(eq(1L))).thenReturn(Optional.of(term));
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student1, student2));
		// Student 1 already has a report card from moments ago — as if an earlier attempt of this
		// same run succeeded for them before a crash triggered the retry now re-running execute().
		when(reportCardRepository.findStudentIdByTermIdAndTenantIdAndCreatedAtGreaterThanEqual(eq(5L), eq(1L), any()))
				.thenReturn(List.of(1L));
		when(tenantAdminNotifier.notifyAll(eq(1L), any(), any())).thenReturn(1);

		JobExecutionResult result = job.execute(context());

		verify(reportCardService, never()).generate(eq(1L), any(), any(), any(), any());
		verify(reportCardService, times(1)).generate(eq(2L), eq(5L), any(), any(), any());
		assertEquals(new JobExecutionResult.Success(Map.of("generatedCount", 2, "notifiedCount", 1), null), result);
	}
}
