package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.school.application.service.LeaveBalanceService;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceAllocationJobTest {

	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private LeaveTypeRepository leaveTypeRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private LeaveBalanceService leaveBalanceService;

	private LeaveBalanceAllocationJob job;

	@BeforeEach
	void setUp() {
		job = new LeaveBalanceAllocationJob(academicYearRepository, leaveTypeRepository, employeeRepository,
				leaveBalanceService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "LeaveBalanceAllocation", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	@Test
	void execute_withNoCurrentAcademicYear_skipsAndReturnsZero() {
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.empty());

		JobExecutionResult result = job.execute(context());

		verify(leaveBalanceService, never()).allocateWithCarryForward(any(), any(), any());
		assertEquals(new JobExecutionResult.Success(Map.of("allocated", 0), null), result);
	}

	@Test
	void execute_allocatesEveryActiveLeaveTypeToEveryEmployee() {
		AcademicYear academicYear = AcademicYear.create("2026-27", LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31), true);
		academicYear.setId(30L);
		LeaveType sick = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));
		sick.setId(20L);
		LeaveType casual = LeaveType.create("Casual Leave", BigDecimal.valueOf(6));
		casual.setId(21L);
		Employee employeeA = Employee.create(StaffCategory.SUPPORT, "EMP-1", "Jane", "Doe", "jane@school.test", null);
		employeeA.setId(10L);
		Employee employeeB = Employee.create(StaffCategory.SUPPORT, "EMP-2", "Sam", "Lee", "sam@school.test", null);
		employeeB.setId(11L);
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.of(academicYear));
		when(leaveTypeRepository.findAllByTenantIdAndActiveTrue(1L)).thenReturn(List.of(sick, casual));
		when(employeeRepository.findAllByTenantIdAndStatus(1L, EmployeeStatus.ACTIVE))
				.thenReturn(List.of(employeeA, employeeB));

		JobExecutionResult result = job.execute(context());

		verify(leaveBalanceService, times(1)).allocateWithCarryForward(employeeA, sick, academicYear);
		verify(leaveBalanceService, times(1)).allocateWithCarryForward(employeeB, sick, academicYear);
		verify(leaveBalanceService, times(1)).allocateWithCarryForward(employeeA, casual, academicYear);
		verify(leaveBalanceService, times(1)).allocateWithCarryForward(employeeB, casual, academicYear);
		assertEquals(new JobExecutionResult.Success(Map.of("allocated", 4), null), result);
	}
}
