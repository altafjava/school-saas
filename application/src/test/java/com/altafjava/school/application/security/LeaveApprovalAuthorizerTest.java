package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveApprovalStage;
import com.altafjava.school.domain.leave.model.LeaveRequest;

@ExtendWith(MockitoExtension.class)
class LeaveApprovalAuthorizerTest {

	private static final Long TENANT_ID = 1L;
	private static final Long REQUESTER_ID = 10L;
	private static final Long ACTOR_USER_ID = 77L;

	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private DepartmentRepository departmentRepository;
	@Mock
	private PermissionAuthorizationService permissionAuthorizationService;

	private LeaveApprovalAuthorizer authorizer;
	private LeaveRequest request;

	@BeforeEach
	void setUp() {
		authorizer = new LeaveApprovalAuthorizer(employeeRepository, departmentRepository,
				permissionAuthorizationService);
		request = LeaveRequest.submit(REQUESTER_ID, 2L, 3L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
				"Trip", BigDecimal.valueOf(2), 2);
		Employee requester = employee(REQUESTER_ID, 40L);
		lenient().when(employeeRepository.findByIdAndTenantId(REQUESTER_ID, TENANT_ID))
				.thenReturn(Optional.of(requester));
	}

	private Employee employee(Long id, Long departmentId) {
		Employee employee = Employee.create(StaffCategory.SUPPORT, "EMP-" + id, "Jane", "Doe",
				"j" + id + "@school.test",
				null);
		employee.setId(id);
		employee.setDepartmentId(departmentId);
		return employee;
	}

	private void departmentHeadedBy(Long headEmployeeId) {
		Department department = Department.create("Science", "SCI", null);
		department.setId(40L);
		department.assignHeadEmployee(headEmployeeId);
		lenient().when(departmentRepository.findByIdAndTenantId(40L, TENANT_ID)).thenReturn(Optional.of(department));
	}

	@Test
	void departmentHeadStage_byTheRequestersDepartmentHead_isAllowed() {
		departmentHeadedBy(11L);
		when(employeeRepository.findByUserIdAndTenantId(ACTOR_USER_ID, TENANT_ID))
				.thenReturn(Optional.of(employee(11L, 40L)));

		assertDoesNotThrow(() -> authorizer.assertMayDecide(TENANT_ID, request, LeaveApprovalStage.DEPARTMENT_HEAD,
				ACTOR_USER_ID));
	}

	@Test
	void departmentHeadStage_byAnotherDepartmentsHead_isDenied() {
		departmentHeadedBy(99L);
		when(employeeRepository.findByUserIdAndTenantId(ACTOR_USER_ID, TENANT_ID))
				.thenReturn(Optional.of(employee(11L, 41L)));

		assertThrows(AccessDeniedException.class, () -> authorizer.assertMayDecide(TENANT_ID, request,
				LeaveApprovalStage.DEPARTMENT_HEAD, ACTOR_USER_ID));
	}

	@Test
	void departmentHeadStage_byAnAdministratorWhoIsNotTheHead_isDenied() {
		departmentHeadedBy(99L);
		when(employeeRepository.findByUserIdAndTenantId(ACTOR_USER_ID, TENANT_ID)).thenReturn(Optional.empty());
		lenient().when(permissionAuthorizationService.hasPermission("LEAVE_REQUEST_MANAGE")).thenReturn(true);

		assertThrows(AccessDeniedException.class, () -> authorizer.assertMayDecide(TENANT_ID, request,
				LeaveApprovalStage.DEPARTMENT_HEAD, ACTOR_USER_ID));
	}

	@Test
	void administratorStage_withTheManagePermission_isAllowed() {
		when(employeeRepository.findByUserIdAndTenantId(ACTOR_USER_ID, TENANT_ID)).thenReturn(Optional.empty());
		when(permissionAuthorizationService.hasPermission("LEAVE_REQUEST_MANAGE")).thenReturn(true);

		assertDoesNotThrow(() -> authorizer.assertMayDecide(TENANT_ID, request, LeaveApprovalStage.ADMINISTRATOR,
				ACTOR_USER_ID));
	}

	@Test
	void administratorStage_byTheDepartmentHeadWithoutTheManagePermission_isDenied() {
		when(employeeRepository.findByUserIdAndTenantId(ACTOR_USER_ID, TENANT_ID))
				.thenReturn(Optional.of(employee(11L, 40L)));
		when(permissionAuthorizationService.hasPermission("LEAVE_REQUEST_MANAGE")).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> authorizer.assertMayDecide(TENANT_ID, request,
				LeaveApprovalStage.ADMINISTRATOR, ACTOR_USER_ID));
	}

	@Test
	void anyStage_byTheRequesterThemselves_isDeniedEvenWithTheManagePermission() {
		when(employeeRepository.findByUserIdAndTenantId(ACTOR_USER_ID, TENANT_ID))
				.thenReturn(Optional.of(employee(REQUESTER_ID, 40L)));
		lenient().when(permissionAuthorizationService.hasPermission("LEAVE_REQUEST_MANAGE")).thenReturn(true);

		assertThrows(AccessDeniedException.class, () -> authorizer.assertMayDecide(TENANT_ID, request,
				LeaveApprovalStage.ADMINISTRATOR, ACTOR_USER_ID));
	}
}
