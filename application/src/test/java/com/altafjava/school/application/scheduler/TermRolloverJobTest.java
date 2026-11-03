package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
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
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class TermRolloverJobTest {

	@Mock
	private TermRepository termRepository;

	private TermRolloverJob job;

	@BeforeEach
	void setUp() {
		job = new TermRolloverJob(termRepository, TestSchoolClocks.systemDefault());
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "TermRollover", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	private Term termWithId(long id, String name, LocalDate startDate, LocalDate endDate) {
		Term term = Term.create(name, startDate, endDate, 1L);
		term.setId(id);
		return term;
	}

	@Test
	void execute_whenCurrentTermTransitionsToAnother_flipsBothFlags() {
		Term outgoing = termWithId(5L, "Term 1", LocalDate.now().minusDays(60), LocalDate.now().minusDays(1));
		outgoing.markCurrent();
		Term incoming = termWithId(6L, "Term 2", LocalDate.now(), LocalDate.now().plusDays(60));
		when(termRepository.findByDateRangeContainingAndTenantId(eq(1L), any(LocalDate.class)))
				.thenReturn(Optional.of(incoming));
		when(termRepository.findCurrentByTenantId(1L)).thenReturn(Optional.of(outgoing));

		JobExecutionResult result = job.execute(context());

		assertTrue(incoming.isCurrent());
		assertEquals(false, outgoing.isCurrent());
		verify(termRepository).save(outgoing);
		verify(termRepository).save(incoming);
		assertEquals(new JobExecutionResult.Success(Map.of("changed", true), null), result);
	}

	@Test
	void execute_whenAlreadyCorrect_doesNothing() {
		Term current = termWithId(5L, "Term 1", LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
		current.markCurrent();
		when(termRepository.findByDateRangeContainingAndTenantId(eq(1L), any(LocalDate.class)))
				.thenReturn(Optional.of(current));
		when(termRepository.findCurrentByTenantId(1L)).thenReturn(Optional.of(current));

		JobExecutionResult result = job.execute(context());

		verify(termRepository, never()).save(any());
		assertEquals(new JobExecutionResult.Success(Map.of("changed", false), null), result);
	}

	@Test
	void execute_whenNoTermCoversTodayButOneWasCurrent_unflagsIt() {
		Term outgoing = termWithId(5L, "Term 1", LocalDate.now().minusDays(60), LocalDate.now().minusDays(1));
		outgoing.markCurrent();
		when(termRepository.findByDateRangeContainingAndTenantId(eq(1L), any(LocalDate.class)))
				.thenReturn(Optional.empty());
		when(termRepository.findCurrentByTenantId(1L)).thenReturn(Optional.of(outgoing));

		JobExecutionResult result = job.execute(context());

		assertEquals(false, outgoing.isCurrent());
		verify(termRepository).save(outgoing);
		assertEquals(new JobExecutionResult.Success(Map.of("changed", true), null), result);
	}

	@Test
	void execute_whenNoneCurrentAndOneNowCovers_flagsIt() {
		Term incoming = termWithId(6L, "Term 2", LocalDate.now(), LocalDate.now().plusDays(60));
		when(termRepository.findByDateRangeContainingAndTenantId(eq(1L), any(LocalDate.class)))
				.thenReturn(Optional.of(incoming));
		when(termRepository.findCurrentByTenantId(1L)).thenReturn(Optional.empty());

		JobExecutionResult result = job.execute(context());

		assertTrue(incoming.isCurrent());
		verify(termRepository).save(incoming);
		assertEquals(new JobExecutionResult.Success(Map.of("changed", true), null), result);
	}
}
