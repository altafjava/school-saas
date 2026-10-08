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
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs 30 minutes after {@link AcademicYearRolloverJob} on April 1st. Allocates each active leave
 * type's default annual days to every employee for the (by then, already rolled-over) current
 * academic year — idempotent, since {@link LeaveBalanceService#allocateIfAbsent} skips any
 * (employee, leave type, academic year) tuple that already has a balance row.
 */
@Slf4j
@Component
@ScheduledJob(name = "LeaveBalanceAllocation", group = "school", description = "Allocates annual leave balances for the new academic year", cronExpression = "0 30 0 1 4 ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class LeaveBalanceAllocationJob implements JobExecutionStrategy {

	private final AcademicYearRepository academicYearRepository;
	private final LeaveTypeRepository leaveTypeRepository;
	private final EmployeeRepository employeeRepository;
	private final LeaveBalanceService leaveBalanceService;

	public LeaveBalanceAllocationJob(AcademicYearRepository academicYearRepository,
			LeaveTypeRepository leaveTypeRepository, EmployeeRepository employeeRepository,
			LeaveBalanceService leaveBalanceService) {
		this.academicYearRepository = academicYearRepository;
		this.leaveTypeRepository = leaveTypeRepository;
		this.employeeRepository = employeeRepository;
		this.leaveBalanceService = leaveBalanceService;
	}

	@Override
	public String jobName() {
		return "LeaveBalanceAllocation";
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
		log.info("action=leave-balance-allocation tenantId={} executionId={}", tenantId, ctx.executionId());

		var academicYear = academicYearRepository.findByCurrentTrueAndTenantId(tenantId).orElse(null);
		if (academicYear == null) {
			log.warn("action=leave-balance-allocation-skipped tenantId={} reason=no-current-academic-year", tenantId);
			return new JobExecutionResult.Success(Map.of("allocated", 0), null);
		}

		var activeLeaveTypes = leaveTypeRepository.findAllByTenantIdAndActiveTrue(tenantId);
		var employees = employeeRepository.findAllByTenantIdAndStatus(tenantId, EmployeeStatus.ACTIVE);

		int allocated = 0;
		for (var leaveType : activeLeaveTypes) {
			for (var employee : employees) {
				leaveBalanceService.allocateWithCarryForward(employee, leaveType, academicYear);
				allocated++;
			}
		}

		log.info("action=leave-balance-allocation-complete tenantId={} allocated={}", tenantId, allocated);
		return new JobExecutionResult.Success(Map.of("allocated", allocated), null);
	}
}
