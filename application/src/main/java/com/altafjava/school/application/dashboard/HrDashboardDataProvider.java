package com.altafjava.school.application.dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.report.provider.ReportDataProvider;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;

/** HR summary: one aggregate row, computed from single COUNT queries. */
@Component
public class HrDashboardDataProvider implements ReportDataProvider {

	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;
	private final LeaveRequestRepository leaveRequestRepository;

	public HrDashboardDataProvider(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
			LeaveRequestRepository leaveRequestRepository) {
		this.employeeRepository = employeeRepository;
		this.departmentRepository = departmentRepository;
		this.leaveRequestRepository = leaveRequestRepository;
	}

	@Override
	public List<Map<String, Object>> fetchData(Map<String, Object> parameters) {
		Long tenantId = TenantContext.getCurrentTenantId();

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("teacherCount", employeeRepository.countByTenantIdAndStaffCategoryAndStatus(tenantId,
				StaffCategory.TEACHING, EmployeeStatus.ACTIVE));
		row.put("employeeCount", employeeRepository.countByTenantIdAndStatus(tenantId, EmployeeStatus.ACTIVE));
		row.put("departmentCount", departmentRepository.countByTenantId(tenantId));
		row.put("pendingLeaveRequestCount",
				leaveRequestRepository.countByTenantIdAndStatus(tenantId, LeaveRequestStatus.PENDING));
		return List.of(row);
	}
}
