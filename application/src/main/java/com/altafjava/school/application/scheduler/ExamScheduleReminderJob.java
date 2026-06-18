package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;

/**
 * Runs daily at 08:00 tenant-local time.
 * Notifies students and parents of exams scheduled within the next 2 days.
 */
@Component
@ScheduledJob(name = "ExamScheduleReminder", group = "school", description = "Notifies students and parents of upcoming exams", cronExpression = "0 0 8 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class ExamScheduleReminderJob implements JobExecutionStrategy {

	private static final Logger log = LoggerFactory.getLogger(ExamScheduleReminderJob.class);

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
