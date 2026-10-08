package com.altafjava.school.application.scheduler;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.service.PayslipService;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs at 01:00 on the 1st of each month, for the just-completed month. Drafts a {@link
 * com.altafjava.school.domain.payroll.model.Payslip} for every employee in the tenant —
 * employees with no active salary structure, or that already have a payslip for the month
 * (re-run after a partial failure), are skipped rather than failing the whole job.
 */
@Slf4j
@Component
@ScheduledJob(name = "PayslipGeneration", group = "school", description = "Generates draft payslips for every active employee for the completed month", cronExpression = "0 0 1 1 * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class PayslipGenerationJob implements JobExecutionStrategy {

	private final EmployeeRepository employeeRepository;
	private final PayslipService payslipService;

	public PayslipGenerationJob(EmployeeRepository employeeRepository, PayslipService payslipService) {
		this.employeeRepository = employeeRepository;
		this.payslipService = payslipService;
	}

	@Override
	public String jobName() {
		return "PayslipGeneration";
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
		YearMonth payMonth = YearMonth.now().minusMonths(1);
		log.info("action=payslip-generation tenantId={} payMonth={} executionId={}", tenantId, payMonth,
				ctx.executionId());

		// Includes anyone who left during the pay month: they are still owed their final payslip.
		List<Employee> employees = employeeRepository.findAllEmployedSince(tenantId, payMonth.atDay(1));
		int generated = 0;
		int skipped = 0;
		for (Employee employee : employees) {
			if (generateForEmployee(employee, payMonth)) {
				generated++;
			} else {
				skipped++;
			}
		}

		log.info("action=payslip-generation-complete tenantId={} payMonth={} generated={} skipped={}", tenantId,
				payMonth, generated, skipped);
		return new JobExecutionResult.Success(Map.of("generated", generated, "skipped", skipped), null);
	}

	private boolean generateForEmployee(Employee employee, YearMonth payMonth) {
		try {
			payslipService.generate(employee.getId(), payMonth);
			return true;
		} catch (BusinessException e) {
			log.warn("action=payslip-generation-skipped employeeId={} payMonth={} reason={}", employee.getId(),
					payMonth, e.getMessage());
			return false;
		}
	}
}
