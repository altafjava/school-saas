package com.altafjava.school.demo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.LeaveBalanceService;
import com.altafjava.school.application.service.LeaveRequestService;
import com.altafjava.school.application.service.LeaveTypeService;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveType;
import lombok.RequiredArgsConstructor;

/** Leave types with balances for every employee, and the demo teacher's approved and pending requests. */
@Component
@Profile({ "dev", "test" })
@RequiredArgsConstructor
class DemoStaffLeaveSeeder {

	private final LeaveTypeService leaveTypeService;
	private final LeaveBalanceService leaveBalanceService;
	private final LeaveRequestService leaveRequestService;

	void seed(DemoSchool school) {
		LeaveType casual = leaveTypeService.create("Casual Leave", new BigDecimal("12"));
		LeaveType sick = leaveTypeService.create("Sick Leave", new BigDecimal("10"));
		List<Employee> everyone = new ArrayList<>(school.teachers());
		everyone.addAll(school.staff());
		for (Employee employee : everyone) {
			for (LeaveType type : List.of(casual, sick)) {
				leaveBalanceService.allocateIfAbsent(employee.getId(), type.getId(), school.year().getId(),
						type.getDefaultAnnualDays());
			}
		}

		Long teacherUserId = school.loginTeacher().getUserId();
		LeaveRequest[] approved = new LeaveRequest[1];
		DemoPrincipal.runAs(teacherUserId, school.tenantId(), SchoolRoles.TEACHER, () -> {
			approved[0] = leaveRequestService.submit(sick.getPublicId().toString(), school.today().minusDays(10),
					school.today().minusDays(9), "Fever");
			leaveRequestService.submit(casual.getPublicId().toString(), school.today().plusDays(14),
					school.today().plusDays(15), "Family function");
		});
		DemoPrincipal.runAs(school.adminUserId(), school.tenantId(), Roles.TENANT_ADMIN,
				() -> leaveRequestService.approve(approved[0].getPublicId().toString()));
	}
}
