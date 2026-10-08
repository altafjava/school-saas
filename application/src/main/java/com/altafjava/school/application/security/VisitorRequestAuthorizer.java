package com.altafjava.school.application.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.visitor.model.VisitorRequest;
import lombok.RequiredArgsConstructor;

/**
 * Who may act on a visitor request: the front desk can raise one for any host; a host raises and
 * decides on their own guests; a security approver decides on anyone's.
 */
@Component
@RequiredArgsConstructor
public class VisitorRequestAuthorizer {

	private final EmployeeRepository employeeRepository;
	private final PermissionAuthorizationService permissionAuthorizationService;

	public void assertMayRaiseFor(Long tenantId, Employee host, Long actorUserId) {
		if (permissionAuthorizationService.hasPermission("VISITOR_LOG_MANAGE")
				|| isHost(tenantId, host.getId(), actorUserId)) {
			return;
		}
		throw new AccessDeniedException("You can only pre-register visitors for yourself");
	}

	public void assertMayDecide(Long tenantId, VisitorRequest request, Long actorUserId) {
		if (permissionAuthorizationService.hasPermission("VISITOR_REQUEST_APPROVE")
				|| isHost(tenantId, request.getHostEmployeeId(), actorUserId)) {
			return;
		}
		throw new AccessDeniedException("Only the host or a visitor-request approver can decide on this visit");
	}

	public void assertMayManage(Long tenantId, VisitorRequest request, Long actorUserId) {
		boolean requester = actorUserId.equals(request.getRequestedByUserId());
		if (requester || permissionAuthorizationService.hasPermission("VISITOR_LOG_MANAGE")
				|| permissionAuthorizationService.hasPermission("VISITOR_REQUEST_APPROVE")
				|| isHost(tenantId, request.getHostEmployeeId(), actorUserId)) {
			return;
		}
		throw new AccessDeniedException("You cannot change this visitor request");
	}

	private boolean isHost(Long tenantId, Long hostEmployeeId, Long actorUserId) {
		return employeeRepository.findByUserIdAndTenantId(actorUserId, tenantId)
				.map(Employee::getId)
				.filter(hostEmployeeId::equals)
				.isPresent();
	}
}
