package com.altafjava.school.application.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveApprovalStage;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import lombok.RequiredArgsConstructor;

/**
 * Decides who may act on a leave request at a given approval stage: the requester's department
 * head at the first level, a leave administrator at the final one — and never the requester
 * themselves, whatever permissions they hold.
 */
@Component
@RequiredArgsConstructor
public class LeaveApprovalAuthorizer {

	private static final String MANAGE_PERMISSION = "LEAVE_REQUEST_MANAGE";

	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;
	private final PermissionAuthorizationService permissionAuthorizationService;

	public void assertMayDecide(Long tenantId, LeaveRequest request, LeaveApprovalStage stage, Long actorUserId) {
		Employee actor = employeeRepository.findByUserIdAndTenantId(actorUserId, tenantId).orElse(null);
		if (actor != null && actor.getId().equals(request.getEmployeeId())) {
			throw new AccessDeniedException("You cannot decide your own leave request");
		}
		boolean allowed = switch (stage) {
			case ADMINISTRATOR -> permissionAuthorizationService.hasPermission(MANAGE_PERMISSION);
			case DEPARTMENT_HEAD -> actor != null && heads(tenantId, actor, request.getEmployeeId());
		};
		if (!allowed) {
			throw new AccessDeniedException("This leave request is awaiting approval from "
					+ (stage == LeaveApprovalStage.ADMINISTRATOR ? "a leave administrator"
							: "the requester's department head"));
		}
	}

	private boolean heads(Long tenantId, Employee actor, Long requesterId) {
		return employeeRepository.findByIdAndTenantId(requesterId, tenantId)
				.map(Employee::getDepartmentId)
				.flatMap(departmentId -> departmentRepository.findByIdAndTenantId(departmentId, tenantId))
				.map(Department::getHeadEmployeeId)
				.filter(actor.getId()::equals)
				.isPresent();
	}
}
