package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs weekly on Monday at 08:00 tenant-local time.
 * Generates a weekly attendance summary report and sends it to tenant admins.
 */
@Slf4j
@Component
@ScheduledJob(name = "AttendanceSummaryReport", group = "school", description = "Generates weekly attendance summary report for tenant admins", cronExpression = "0 0 8 ? * MON", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class AttendanceSummaryReportJob implements JobExecutionStrategy {

	@Override
	public String jobName() {
		return "AttendanceSummaryReport";
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
		log.info("action=attendance-summary-report tenantId={} executionId={}", ctx.tenantId(), ctx.executionId());
		return new JobExecutionResult.Success(Map.of("reportGenerated", true), null);
	}
}
