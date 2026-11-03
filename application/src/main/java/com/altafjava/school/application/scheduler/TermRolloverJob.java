package com.altafjava.school.application.scheduler;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 00:05 tenant-local time. Unlike academic years (one fixed April 1st rollover per
 * year), term date ranges vary per tenant and per academic year, so "the current term" must be
 * re-evaluated every day: un-flags whichever term was previously current once its date range no
 * longer contains today, and flags whichever term's date range now contains today.
 */
@Slf4j
@Component
@ScheduledJob(name = "TermRollover", group = "school", description = "Re-evaluates which term is current based on today's date", cronExpression = "0 5 0 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class TermRolloverJob implements JobExecutionStrategy {

	private final TermRepository termRepository;
	private final SchoolClock schoolClock;

	public TermRolloverJob(TermRepository termRepository, SchoolClock schoolClock) {
		this.termRepository = termRepository;
		this.schoolClock = schoolClock;
	}

	@Override
	public String jobName() {
		return "TermRollover";
	}

	@Override
	public String jobGroup() {
		return "school";
	}

	@Override
	public boolean isTenantScoped() {
		return true;
	}

	@Override
	@Transactional
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		log.info("action=term-rollover tenantId={} executionId={}", tenantId, ctx.executionId());

		LocalDate today = schoolClock.today(tenantId);
		Optional<Term> dueToBeCurrent = termRepository.findByDateRangeContainingAndTenantId(tenantId, today);
		Optional<Term> currentlyMarked = termRepository.findCurrentByTenantId(tenantId);

		boolean alreadyCorrect = currentlyMarked.isPresent() && dueToBeCurrent.isPresent()
				&& currentlyMarked.get().getId().equals(dueToBeCurrent.get().getId());
		if (alreadyCorrect) {
			return new JobExecutionResult.Success(Map.of("changed", false), null);
		}

		currentlyMarked.ifPresent(term -> {
			term.markNotCurrent();
			termRepository.save(term);
			log.info("action=term-deactivated tenantId={} term={}", tenantId, term.getName());
		});

		dueToBeCurrent.ifPresent(term -> {
			term.markCurrent();
			termRepository.save(term);
			log.info("action=term-activated tenantId={} term={}", tenantId, term.getName());
		});

		return new JobExecutionResult.Success(Map.of("changed", true), null);
	}
}
