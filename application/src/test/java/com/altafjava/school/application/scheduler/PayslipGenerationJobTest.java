package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.school.application.service.PayslipService;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class PayslipGenerationJobTest {

	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private PayslipService payslipService;

	private PayslipGenerationJob job;

	@BeforeEach
	void setUp() {
		job = new PayslipGenerationJob(employeeRepository, payslipService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "PayslipGeneration", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	private Employee employee(long id) {
		Employee employee = Employee.create(StaffCategory.SUPPORT, "EMP-" + id, "Jane", "Doe",
				"jane" + id + "@school.test",
				LocalDate.of(2020, 1, 1));
		employee.setId(id);
		return employee;
	}

	@Test
	void execute_generatesPayslipForEveryEmployee() {
		when(employeeRepository.findAllEmployedSince(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(employee(10L), employee(11L)));

		JobExecutionResult result = job.execute(context());

		YearMonth expectedMonth = YearMonth.now().minusMonths(1);
		verify(payslipService, times(1)).generate(eq(10L), eq(expectedMonth));
		verify(payslipService, times(1)).generate(eq(11L), eq(expectedMonth));
		assertEquals(new JobExecutionResult.Success(Map.of("generated", 2, "skipped", 0), null), result);
	}

	@Test
	void execute_skipsEmployeeWithoutActiveSalaryStructureRatherThanFailing() {
		when(employeeRepository.findAllEmployedSince(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(employee(10L), employee(11L)));
		doThrow(new BusinessException("No active salary structure for employee 10")).when(payslipService)
				.generate(eq(10L), any(YearMonth.class));

		JobExecutionResult result = job.execute(context());

		assertEquals(new JobExecutionResult.Success(Map.of("generated", 1, "skipped", 1), null), result);
	}

	@Test
	void execute_withNoEmployees_returnsZeroCounts() {
		when(employeeRepository.findAllEmployedSince(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

		JobExecutionResult result = job.execute(context());

		assertEquals(new JobExecutionResult.Success(Map.of("generated", 0, "skipped", 0), null), result);
	}
}
