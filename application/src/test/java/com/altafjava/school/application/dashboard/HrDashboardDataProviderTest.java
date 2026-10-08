package com.altafjava.school.application.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class HrDashboardDataProviderTest {

	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private DepartmentRepository departmentRepository;
	@Mock
	private LeaveRequestRepository leaveRequestRepository;

	private HrDashboardDataProvider provider;

	@BeforeEach
	void setUp() {
		provider = new HrDashboardDataProvider(employeeRepository, departmentRepository, leaveRequestRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void fetchData_returnsSingleSummaryRow() {
		when(employeeRepository.countByTenantIdAndStatus(1L, EmployeeStatus.ACTIVE)).thenReturn(45L);
		when(employeeRepository.countByTenantIdAndStaffCategoryAndStatus(1L, StaffCategory.TEACHING,
				EmployeeStatus.ACTIVE)).thenReturn(30L);
		when(departmentRepository.countByTenantId(1L)).thenReturn(6L);
		when(leaveRequestRepository.countByTenantIdAndStatus(1L, LeaveRequestStatus.PENDING)).thenReturn(4L);

		List<Map<String, Object>> result = provider.fetchData(Map.of());

		assertEquals(1, result.size());
		Map<String, Object> row = result.get(0);
		assertEquals(45L, row.get("employeeCount"));
		assertEquals(30L, row.get("teacherCount"));
		assertEquals(6L, row.get("departmentCount"));
		assertEquals(4L, row.get("pendingLeaveRequestCount"));
	}
}
