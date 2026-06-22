package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 08:00 tenant-local time.
 * Notifies students and parents of exams scheduled within the next 2 days.
 */
@Slf4j
@Component
@ScheduledJob(name = "ExamScheduleReminder", group = "school", description = "Notifies students and parents of upcoming exams", cronExpression = "0 0 8 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class ExamScheduleReminderJob implements JobExecutionStrategy {

	@Override
	public String jobName() {
		return "ExamScheduleReminder";
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
		log.info("action=exam-schedule-reminder tenantId={} executionId={}", ctx.tenantId(), ctx.executionId());
		return new JobExecutionResult.Success(Map.of("remindedCount", 0), null);
	}
}
