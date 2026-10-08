package com.altafjava.school.application.scheduler;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.service.BookReservationService;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily: a book held for a reservation that nobody collected within the hold period goes back
 * to the shelf, or to the next member in the queue. Idempotent — an already-expired hold is closed
 * and no longer matches.
 */
@Slf4j
@Component
@ScheduledJob(name = "LibraryReservationExpiry", group = "school", description = "Releases reserved books left uncollected past their hold period", cronExpression = "0 30 1 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class LibraryReservationExpiryJob implements JobExecutionStrategy {

	private final BookReservationService bookReservationService;

	public LibraryReservationExpiryJob(BookReservationService bookReservationService) {
		this.bookReservationService = bookReservationService;
	}

	@Override
	public String jobName() {
		return "LibraryReservationExpiry";
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
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		int expired = bookReservationService.expireLapsedHolds(tenantId, LocalDate.now());
		log.info("action=library-reservation-expiry-complete tenantId={} expired={}", tenantId, expired);
		return new JobExecutionResult.Success(Map.of("expired", expired), null);
	}
}
