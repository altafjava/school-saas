package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.service.LeaveBalanceService;
import com.altafjava.school.application.time.SchoolClock;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily: forfeits any {@code LeaveBalance}'s carried-forward days that are still unused past
 * their {@code carryForwardExpiresAt} date (see {@code LeaveType#configureCarryForward} and
 * {@code LeaveBalance#forfeitExpiredCarryForward}) — idempotent, a balance with no expiry set or
 * one already forfeited is simply skipped on the next run.
 */
@Slf4j
@Component
@ScheduledJob(name = "LeaveCarryForwardExpiry", group = "school", description = "Forfeits unused leave carry-forward days past their expiry date", cronExpression = "0 0 1 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class LeaveCarryForwardExpiryJob implements JobExecutionStrategy {

	private final LeaveBalanceService leaveBalanceService;
	private final SchoolClock schoolClock;

	public LeaveCarryForwardExpiryJob(LeaveBalanceService leaveBalanceService, SchoolClock schoolClock) {
		this.leaveBalanceService = leaveBalanceService;
		this.schoolClock = schoolClock;
	}

	@Override
	public String jobName() {
		return "LeaveCarryForwardExpiry";
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
		int forfeited = leaveBalanceService.forfeitExpiredCarryForward(tenantId, schoolClock.today(tenantId));
		log.info("action=leave-carry-forward-expiry-complete tenantId={} forfeited={}", tenantId, forfeited);
		return new JobExecutionResult.Success(Map.of("forfeited", forfeited), null);
	}
}
