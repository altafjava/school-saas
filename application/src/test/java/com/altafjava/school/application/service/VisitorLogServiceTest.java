package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.visitor.VisitorBadgeIssuer;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.visitor.model.VisitorLog;
import com.altafjava.school.domain.visitor.model.VisitorRequest;
import com.altafjava.school.domain.visitor.model.VisitorRequestStatus;
import com.altafjava.school.domain.visitor.repository.VisitorLogRepository;
import com.altafjava.school.domain.visitor.repository.VisitorRequestRepository;

@ExtendWith(MockitoExtension.class)
class VisitorLogServiceTest {

	private static final Long TENANT_ID = 1L;
	private static final Long USER_ID = 5L;
	private static final UUID REQUEST_PUBLIC_ID = UUID.randomUUID();
	private static final UUID PHOTO = UUID.randomUUID();

	@Mock
	private VisitorLogRepository visitorLogRepository;
	@Mock
	private VisitorRequestRepository visitorRequestRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private VisitorBadgeIssuer visitorBadgeIssuer;
	@Mock
	private DocumentIssuanceService documentIssuanceService;
	@Mock
	private PlatformTransactionManager transactionManager;

	private VisitorLogService service;
	private VisitorRequest request;
	private Employee host;
	private DocumentIssuance badge;

	@BeforeEach
	void setUp() {
		lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
		service = new VisitorLogService(visitorLogRepository, visitorRequestRepository, employeeRepository,
				visitorBadgeIssuer, documentIssuanceService, transactionManager);
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
		request = VisitorRequest.walkIn("Alex Ray", "555-0100", "Meeting", 20L, 3L, LocalDate.now());
		request.setId(40L);
		request.approve(7L, LocalDate.now());
		host = Employee.create(StaffCategory.SUPPORT, "EMP-1", "Jane", "Doe", "jane@school.test", null);
		host.setId(20L);
		badge = DocumentIssuance.create("VISITOR_BADGE", "VISITOR_REQUEST", 40L, "Visitor Badge", "Alex Ray", null,
				null, "code1234", "key", USER_ID);
		badge.setId(900L);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void stubAdmissible() {
		when(visitorRequestRepository.findByPublicIdAndTenantId(REQUEST_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.of(request));
		when(employeeRepository.findByIdAndTenantId(20L, TENANT_ID)).thenReturn(Optional.of(host));
	}

	private void stubIssuing() {
		when(visitorBadgeIssuer.issue(eq(TENANT_ID), eq(request), eq(host), eq(PHOTO), any(LocalDate.class),
				eq(USER_ID))).thenReturn(badge);
		when(visitorRequestRepository.findByIdAndTenantId(40L, TENANT_ID)).thenReturn(Optional.of(request));
	}

	private void stubIssuingAndSaving() {
		stubIssuing();
		when(visitorLogRepository.save(any(VisitorLog.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@Test
	void checkIn_againstAnApprovedRequestWithAPhoto_issuesABadgeAndRecordsTheVisit() {
		stubAdmissible();
		stubIssuingAndSaving();

		VisitorLog log = service.checkIn(REQUEST_PUBLIC_ID.toString(), PHOTO.toString(), USER_ID);

		assertEquals("Alex Ray", log.getVisitorName());
		assertEquals(900L, log.getBadgeIssuanceId());
		assertEquals(PHOTO, log.getPhotoFilePublicId());
		assertEquals(VisitorRequestStatus.CHECKED_IN, request.getStatus());
		verify(visitorRequestRepository).save(request);
	}

	@Test
	void checkIn_withoutAPhotoSuppliedAtTheGate_usesTheOneOnTheRequest() {
		request.attachPhoto(PHOTO);
		stubAdmissible();
		stubIssuingAndSaving();

		VisitorLog log = service.checkIn(REQUEST_PUBLIC_ID.toString(), null, USER_ID);

		assertEquals(PHOTO, log.getPhotoFilePublicId());
	}

	@Test
	void checkIn_withNoPhotoAnywhere_throwsBusinessException() {
		when(visitorRequestRepository.findByPublicIdAndTenantId(REQUEST_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.of(request));

		assertThrows(BusinessException.class, () -> service.checkIn(REQUEST_PUBLIC_ID.toString(), null, USER_ID));

		verify(visitorBadgeIssuer, never()).issue(any(), any(), any(), any(), any(), any());
	}

	@Test
	void checkIn_againstARequestThatIsNotApproved_throwsBusinessException() {
		VisitorRequest pending = VisitorRequest.walkIn("Alex Ray", null, "Meeting", 20L, 3L, LocalDate.now());
		when(visitorRequestRepository.findByPublicIdAndTenantId(REQUEST_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.of(pending));

		assertThrows(BusinessException.class,
				() -> service.checkIn(REQUEST_PUBLIC_ID.toString(), PHOTO.toString(), USER_ID));

		verify(visitorBadgeIssuer, never()).issue(any(), any(), any(), any(), any(), any());
	}

	@Test
	void checkIn_whenTheHostHasLeft_throwsBusinessException() {
		host.exit(EmployeeStatus.RESIGNED, LocalDate.now(), null);
		stubAdmissible();

		assertThrows(BusinessException.class,
				() -> service.checkIn(REQUEST_PUBLIC_ID.toString(), PHOTO.toString(), USER_ID));
	}

	@Test
	void checkIn_unknownRequest_throwsResourceNotFound() {
		when(visitorRequestRepository.findByPublicIdAndTenantId(REQUEST_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.checkIn(REQUEST_PUBLIC_ID.toString(), PHOTO.toString(), USER_ID));
	}

	@Test
	void checkIn_whenTheRecordCannotBeSaved_revokesTheBadgeItJustIssued() {
		stubAdmissible();
		stubIssuing();
		doThrow(new IllegalStateException("db down")).when(visitorLogRepository).save(any(VisitorLog.class));

		assertThrows(IllegalStateException.class,
				() -> service.checkIn(REQUEST_PUBLIC_ID.toString(), PHOTO.toString(), USER_ID));

		verify(documentIssuanceService).revoke(eq(badge), anyString());
	}

	private VisitorLog loggedVisitWithBadge(UUID publicId) {
		VisitorLog log = VisitorLog.checkIn(request, PHOTO, LocalDateTime.of(2026, 5, 1, 9, 0));
		log.setPublicId(publicId);
		log.attachBadge(900L);
		when(visitorLogRepository.findByPublicIdAndTenantId(publicId, TENANT_ID)).thenReturn(Optional.of(log));
		when(visitorLogRepository.save(any(VisitorLog.class))).thenAnswer(inv -> inv.getArgument(0));
		return log;
	}

	@Test
	void checkOut_setsCheckOutAtAndRevokesTheBadge() {
		UUID publicId = UUID.randomUUID();
		loggedVisitWithBadge(publicId);
		when(documentIssuanceService.findById(900L)).thenReturn(Optional.of(badge));

		VisitorLog checkedOut = service.checkOut(publicId.toString());

		assertNotNull(checkedOut.getCheckOutAt());
		verify(documentIssuanceService).revoke(eq(badge), anyString());
	}

	@Test
	void checkOut_whenTheBadgeCannotBeRevoked_stillChecksTheVisitorOut() {
		UUID publicId = UUID.randomUUID();
		loggedVisitWithBadge(publicId);
		when(documentIssuanceService.findById(900L)).thenReturn(Optional.of(badge));
		doThrow(new IllegalStateException("storage down")).when(documentIssuanceService).revoke(eq(badge),
				anyString());

		VisitorLog checkedOut = service.checkOut(publicId.toString());

		assertNotNull(checkedOut.getCheckOutAt());
	}

	@Test
	void checkOut_aVisitWithoutABadge_justChecksOut() {
		UUID publicId = UUID.randomUUID();
		VisitorLog legacy = VisitorLog.checkIn(request, PHOTO, LocalDateTime.of(2026, 5, 1, 9, 0));
		when(visitorLogRepository.findByPublicIdAndTenantId(publicId, TENANT_ID)).thenReturn(Optional.of(legacy));
		when(visitorLogRepository.save(any(VisitorLog.class))).thenAnswer(inv -> inv.getArgument(0));

		service.checkOut(publicId.toString());

		verify(documentIssuanceService, never()).findById(any());
	}

	@Test
	void findBadge_aVisitWithoutOne_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		VisitorLog withoutBadge = VisitorLog.checkIn(request, PHOTO, LocalDateTime.of(2026, 5, 1, 9, 0));
		when(visitorLogRepository.findByPublicIdAndTenantId(publicId, TENANT_ID))
				.thenReturn(Optional.of(withoutBadge));

		assertThrows(ResourceNotFoundException.class, () -> service.findBadge(publicId.toString()));
	}
}
