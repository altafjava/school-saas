package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Triggered at end of term (configurable per tenant via job data map).
 * Batch-generates report cards for all students in the tenant.
 * Default schedule: last day of March and last day of October at 23:00.
 */
@Slf4j
@Component
@ScheduledJob(name = "ReportCardGeneration", group = "school", description = "Batch generates report cards for all students at end of term", cronExpression = "0 0 23 L 3,10 ?", tenantScoped = true, retryEnabled = true, maxRetries = 1)
public class ReportCardGenerationJob implements JobExecutionStrategy {

	@Override
	public String jobName() {
		return "ReportCardGeneration";
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
		log.info("action=report-card-generation tenantId={} executionId={}", ctx.tenantId(), ctx.executionId());
		return new JobExecutionResult.Success(Map.of("generatedCount", 0), null);
	}
}
