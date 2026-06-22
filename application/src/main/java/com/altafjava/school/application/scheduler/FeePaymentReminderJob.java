package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 09:00 tenant-local time.
 * Sends fee payment reminders to students/parents whose fees are due within 3 days.
 */
@Slf4j
@Component
@ScheduledJob(name = "FeePaymentReminder", group = "school", description = "Reminds parents/students of upcoming fee due dates", cronExpression = "0 0 9 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class FeePaymentReminderJob implements JobExecutionStrategy {

	@Override
	public String jobName() {
		return "FeePaymentReminder";
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
		log.info("action=fee-payment-reminder tenantId={} executionId={}", ctx.tenantId(), ctx.executionId());
		return new JobExecutionResult.Success(Map.of("remindedCount", 0), null);
	}
}
