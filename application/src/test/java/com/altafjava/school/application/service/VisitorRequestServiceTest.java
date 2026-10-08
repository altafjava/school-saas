package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.security.VisitorRequestAuthorizer;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.visitor.model.VisitorRequest;
import com.altafjava.school.domain.visitor.model.VisitorRequestSource;
import com.altafjava.school.domain.visitor.model.VisitorRequestStatus;
import com.altafjava.school.domain.visitor.repository.VisitorRequestRepository;

@ExtendWith(MockitoExtension.class)
class VisitorRequestServiceTest {

	private static final Long TENANT_ID = 1L;
	private static final Long ACTOR = 5L;
	private static final UUID HOST_PUBLIC_ID = UUID.randomUUID();
	private static final UUID REQUEST_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private VisitorRequestRepository visitorRequestRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private VisitorRequestAuthorizer authorizer;
	@Mock
	private NotificationService notificationService;

	private VisitorRequestService service;
	private Employee host;

	@BeforeEach
	void setUp() {
		service = new VisitorRequestService(visitorRequestRepository, employeeRepository, authorizer,
				notificationService);
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
		host = Employee.create(StaffCategory.SUPPORT, "EMP-1", "Jane", "Doe", "jane@school.test", null);
		host.setId(20L);
		host.setUserId(300L);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void stubHostAndSave() {
		when(employeeRepository.findByPublicIdAndTenantId(HOST_PUBLIC_ID, TENANT_ID)).thenReturn(Optional.of(host));
		when(visitorRequestRepository.save(any(VisitorRequest.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private VisitorRequest pendingRequest() {
		VisitorRequest request = VisitorRequest.walkIn("Alex Ray", null, "Meeting", 20L, ACTOR, LocalDate.now());
		when(visitorRequestRepository.findByPublicIdAndTenantId(REQUEST_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.of(request));
		return request;
	}

	@Test
	void raise_withoutADate_isAWalkInForTodayAndNotifiesTheHost() {
		stubHostAndSave();

		VisitorRequest request = service.raise("Alex Ray", "555", "Meeting", HOST_PUBLIC_ID.toString(), null, ACTOR);

		assertEquals(VisitorRequestSource.WALK_IN, request.getSource());
		assertEquals(LocalDate.now(), request.getVisitDate());
		assertEquals(VisitorRequestStatus.PENDING, request.getStatus());
		verify(authorizer).assertMayRaiseFor(TENANT_ID, host, ACTOR);
		verify(notificationService).send(any(SendNotificationCommand.class));
	}

	@Test
	void raise_withALaterDate_isPreRegistered() {
		stubHostAndSave();
		LocalDate friday = LocalDate.now().plusDays(4);

		VisitorRequest request = service.raise("Alex Ray", null, "Meeting", HOST_PUBLIC_ID.toString(), friday,
				ACTOR);

		assertEquals(VisitorRequestSource.PRE_REGISTERED, request.getSource());
		assertEquals(friday, request.getVisitDate());
	}

	@Test
	void raise_forAHostWithNoLogin_savesWithoutNotifying() {
		host.setUserId(null);
		stubHostAndSave();

		service.raise("Alex Ray", null, "Meeting", HOST_PUBLIC_ID.toString(), null, ACTOR);

		verify(notificationService, never()).send(any(SendNotificationCommand.class));
	}

	@Test
	void raise_forAHostWhoHasLeft_throwsBusinessException() {
		host.exit(EmployeeStatus.RESIGNED, LocalDate.now(), null);
		when(employeeRepository.findByPublicIdAndTenantId(HOST_PUBLIC_ID, TENANT_ID)).thenReturn(Optional.of(host));

		assertThrows(BusinessException.class,
				() -> service.raise("Alex Ray", null, "Meeting", HOST_PUBLIC_ID.toString(), null, ACTOR));
	}

	@Test
	void raise_forAnUnknownHost_throwsResourceNotFound() {
		when(employeeRepository.findByPublicIdAndTenantId(HOST_PUBLIC_ID, TENANT_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.raise("Alex Ray", null, "Meeting", HOST_PUBLIC_ID.toString(), null, ACTOR));
	}

	@Test
	void raise_whenTheAuthorizerRefuses_savesNothing() {
		when(employeeRepository.findByPublicIdAndTenantId(HOST_PUBLIC_ID, TENANT_ID)).thenReturn(Optional.of(host));
		doThrow(new AccessDeniedException("not yours")).when(authorizer).assertMayRaiseFor(TENANT_ID, host, ACTOR);

		assertThrows(AccessDeniedException.class,
				() -> service.raise("Alex Ray", null, "Meeting", HOST_PUBLIC_ID.toString(), null, ACTOR));
		verify(visitorRequestRepository, never()).save(any());
	}

	@Test
	void approve_afterTheAuthorizerAgrees_approvesTheRequest() {
		VisitorRequest request = pendingRequest();
		when(visitorRequestRepository.save(any(VisitorRequest.class))).thenAnswer(inv -> inv.getArgument(0));

		VisitorRequest approved = service.approve(REQUEST_PUBLIC_ID.toString(), ACTOR);

		assertEquals(VisitorRequestStatus.APPROVED, approved.getStatus());
		verify(authorizer).assertMayDecide(TENANT_ID, request, ACTOR);
	}

	@Test
	void approve_whenTheAuthorizerRefuses_leavesTheRequestPending() {
		VisitorRequest request = pendingRequest();
		doThrow(new AccessDeniedException("no")).when(authorizer).assertMayDecide(TENANT_ID, request, ACTOR);

		assertThrows(AccessDeniedException.class, () -> service.approve(REQUEST_PUBLIC_ID.toString(), ACTOR));

		assertEquals(VisitorRequestStatus.PENDING, request.getStatus());
	}

	@Test
	void reject_recordsTheReason() {
		pendingRequest();
		when(visitorRequestRepository.save(any(VisitorRequest.class))).thenAnswer(inv -> inv.getArgument(0));

		VisitorRequest rejected = service.reject(REQUEST_PUBLIC_ID.toString(), "Not expected", ACTOR);

		assertEquals("Not expected", rejected.getDecisionReason());
	}

	@Test
	void cancelAndAttachPhoto_goThroughTheManageCheck() {
		VisitorRequest request = pendingRequest();
		when(visitorRequestRepository.save(any(VisitorRequest.class))).thenAnswer(inv -> inv.getArgument(0));
		UUID photo = UUID.randomUUID();

		service.attachPhoto(REQUEST_PUBLIC_ID.toString(), photo.toString(), ACTOR);
		service.cancel(REQUEST_PUBLIC_ID.toString(), ACTOR);

		assertEquals(photo, request.getPhotoFilePublicId());
		assertEquals(VisitorRequestStatus.CANCELLED, request.getStatus());
		verify(authorizer, org.mockito.Mockito.times(2)).assertMayManage(TENANT_ID, request, ACTOR);
	}

	@Test
	void findByPublicId_unknown_throwsResourceNotFound() {
		when(visitorRequestRepository.findByPublicIdAndTenantId(REQUEST_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.findByPublicId(REQUEST_PUBLIC_ID.toString(), ACTOR));
	}
}
