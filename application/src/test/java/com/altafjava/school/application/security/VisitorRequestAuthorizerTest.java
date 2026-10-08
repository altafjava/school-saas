package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.visitor.model.VisitorRequest;

@ExtendWith(MockitoExtension.class)
class VisitorRequestAuthorizerTest {

	private static final Long TENANT_ID = 1L;
	private static final Long HOST_ID = 20L;
	private static final Long ACTOR = 77L;
	private static final Long REQUESTER = 88L;

	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private PermissionAuthorizationService permissions;

	private VisitorRequestAuthorizer authorizer;
	private Employee host;
	private VisitorRequest request;

	@BeforeEach
	void setUp() {
		authorizer = new VisitorRequestAuthorizer(employeeRepository, permissions);
		host = employee(HOST_ID);
		request = VisitorRequest.walkIn("Alex Ray", null, "Meeting", HOST_ID, REQUESTER, LocalDate.now());
		lenient().when(permissions.hasPermission(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
	}

	private Employee employee(Long id) {
		Employee employee = Employee.create(StaffCategory.SUPPORT, "EMP-" + id, "Jane", "Doe", id + "@school.test",
				null);
		employee.setId(id);
		return employee;
	}

	private void actorIsEmployee(Long employeeId) {
		when(employeeRepository.findByUserIdAndTenantId(ACTOR, TENANT_ID))
				.thenReturn(Optional.of(employee(employeeId)));
	}

	private void actorHasNoEmployeeRecord() {
		when(employeeRepository.findByUserIdAndTenantId(ACTOR, TENANT_ID)).thenReturn(Optional.empty());
	}

	@Test
	void raise_theDeskMayRaiseForAnyHost() {
		when(permissions.hasPermission("VISITOR_LOG_MANAGE")).thenReturn(true);

		assertDoesNotThrow(() -> authorizer.assertMayRaiseFor(TENANT_ID, host, ACTOR));
	}

	@Test
	void raise_aHostMayRaiseForThemselves() {
		actorIsEmployee(HOST_ID);

		assertDoesNotThrow(() -> authorizer.assertMayRaiseFor(TENANT_ID, host, ACTOR));
	}

	@Test
	void raise_aHostMayNotRaiseForSomeoneElse() {
		actorIsEmployee(21L);

		assertThrows(AccessDeniedException.class, () -> authorizer.assertMayRaiseFor(TENANT_ID, host, ACTOR));
	}

	@Test
	void decide_theHostMayDecideOnTheirOwnGuests() {
		actorIsEmployee(HOST_ID);

		assertDoesNotThrow(() -> authorizer.assertMayDecide(TENANT_ID, request, ACTOR));
	}

	@Test
	void decide_anApproverMayDecideOnAnyRequest() {
		when(permissions.hasPermission("VISITOR_REQUEST_APPROVE")).thenReturn(true);

		assertDoesNotThrow(() -> authorizer.assertMayDecide(TENANT_ID, request, ACTOR));
	}

	@Test
	void decide_theFrontDeskAloneMayNotApprove() {
		lenient().when(permissions.hasPermission("VISITOR_LOG_MANAGE")).thenReturn(true);
		actorHasNoEmployeeRecord();

		assertThrows(AccessDeniedException.class, () -> authorizer.assertMayDecide(TENANT_ID, request, ACTOR));
	}

	@Test
	void decide_anotherEmployeeMayNot() {
		actorIsEmployee(21L);

		assertThrows(AccessDeniedException.class, () -> authorizer.assertMayDecide(TENANT_ID, request, ACTOR));
	}

	@Test
	void manage_theRequesterTheDeskTheApproverAndTheHostAreAllowed() {
		assertDoesNotThrow(() -> authorizer.assertMayManage(TENANT_ID, request, REQUESTER));

		when(permissions.hasPermission("VISITOR_LOG_MANAGE")).thenReturn(true);
		assertDoesNotThrow(() -> authorizer.assertMayManage(TENANT_ID, request, ACTOR));
	}

	@Test
	void manage_anUnrelatedEmployeeIsRefused() {
		actorIsEmployee(21L);

		assertThrows(AccessDeniedException.class, () -> authorizer.assertMayManage(TENANT_ID, request, ACTOR));
	}
}
