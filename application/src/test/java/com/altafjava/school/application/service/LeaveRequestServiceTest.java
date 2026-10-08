package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveBalance;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveBalanceRepository;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

	private static final Long CURRENT_USER_ID = 55L;
	private static final UUID LEAVE_TYPE_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private LeaveRequestRepository leaveRequestRepository;
	@Mock
	private LeaveTypeRepository leaveTypeRepository;
	@Mock
	private LeaveBalanceRepository leaveBalanceRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private TenantAdminNotifier tenantAdminNotifier;
	@Mock
	private NotificationService notificationService;
	@Mock
	private HolidayService holidayService;

	private LeaveRequestService leaveRequestService;

	@BeforeEach
	void setUp() {
		leaveRequestService = new LeaveRequestService(leaveRequestRepository, leaveTypeRepository,
				leaveBalanceRepository, employeeRepository, academicYearRepository, tenantAdminNotifier,
				notificationService, holidayService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	private Employee employeeWithId(long id) {
		Employee employee = Employee.create(StaffCategory.SUPPORT, "EMP-1", "Jane", "Doe", "jane@school.test", null);
		employee.setId(id);
		employee.setUserId(CURRENT_USER_ID);
		return employee;
	}

	private LeaveType leaveTypeWithId(long id) {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));
		leaveType.setId(id);
		return leaveType;
	}

	private AcademicYear academicYearWithId(long id) {
		AcademicYear academicYear = AcademicYear.create("2026-27", LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31), true);
		academicYear.setId(id);
		return academicYear;
	}

	@Test
	void submit_withNoExistingBalance_succeedsAndNotifiesAdmins() {
		authenticateAsUser(CURRENT_USER_ID);
		Employee employee = employeeWithId(10L);
		LeaveType leaveType = leaveTypeWithId(20L);
		AcademicYear academicYear = academicYearWithId(30L);
		when(employeeRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(employee));
		when(leaveTypeRepository.findByPublicIdAndTenantId(LEAVE_TYPE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(leaveType));
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.of(academicYear));
		when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(10L, 20L, 30L, 1L))
				.thenReturn(Optional.empty());
		when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

		LeaveRequest request = assertDoesNotThrow(() -> leaveRequestService.submit(LEAVE_TYPE_PUBLIC_ID.toString(),
				LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Personal"));

		assertEquals(10L, request.getEmployeeId());
		verify(tenantAdminNotifier, times(1)).notifyAll(eq(1L), any(), any(), any(), any());
	}

	@Test
	void submit_withInsufficientBalance_throwsBusinessException() {
		authenticateAsUser(CURRENT_USER_ID);
		Employee employee = employeeWithId(10L);
		LeaveType leaveType = leaveTypeWithId(20L);
		AcademicYear academicYear = academicYearWithId(30L);
		LeaveBalance balance = LeaveBalance.allocate(10L, 20L, 30L, BigDecimal.ONE);
		when(employeeRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L)).thenReturn(Optional.of(employee));
		when(leaveTypeRepository.findByPublicIdAndTenantId(LEAVE_TYPE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(leaveType));
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.of(academicYear));
		when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(10L, 20L, 30L, 1L))
				.thenReturn(Optional.of(balance));

		assertThrows(BusinessException.class, () -> leaveRequestService.submit(LEAVE_TYPE_PUBLIC_ID.toString(),
				LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), "Personal"));

		verify(leaveRequestRepository, never()).save(any());
	}

	@Test
	void approve_deductsBalanceAndSetsApprovedStatus() {
		UUID requestPublicId = UUID.randomUUID();
		authenticateAsUser(CURRENT_USER_ID);
		LeaveRequest request = LeaveRequest.submit(10L, 20L, 30L, LocalDate.now().plusDays(1),
				LocalDate.now().plusDays(2), "Personal", BigDecimal.valueOf(2));
		LeaveBalance balance = LeaveBalance.allocate(10L, 20L, 30L, BigDecimal.TEN);
		when(leaveRequestRepository.findByPublicIdAndTenantId(requestPublicId, 1L)).thenReturn(Optional.of(request));
		when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(10L, 20L, 30L, 1L))
				.thenReturn(Optional.of(balance));
		when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));
		when(employeeRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(employeeWithId(10L)));

		LeaveRequest approved = assertDoesNotThrow(() -> leaveRequestService.approve(requestPublicId.toString()));

		assertEquals(0, BigDecimal.valueOf(8).compareTo(balance.remainingDays()));
		verify(notificationService, times(1)).send(any(SendNotificationCommand.class));
	}

	@Test
	void approve_withNoBalanceAllocated_throwsBusinessException() {
		UUID requestPublicId = UUID.randomUUID();
		LeaveRequest request = LeaveRequest.submit(10L, 20L, 30L, LocalDate.now().plusDays(1),
				LocalDate.now().plusDays(2), "Personal", BigDecimal.valueOf(2));
		when(leaveRequestRepository.findByPublicIdAndTenantId(requestPublicId, 1L)).thenReturn(Optional.of(request));
		when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(10L, 20L, 30L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(BusinessException.class, () -> leaveRequestService.approve(requestPublicId.toString()));
	}

	@Test
	void cancel_byOwningEmployee_succeeds() {
		UUID requestPublicId = UUID.randomUUID();
		authenticateAsUser(CURRENT_USER_ID);
		LeaveRequest request = LeaveRequest.submit(10L, 20L, 30L, LocalDate.now().plusDays(3),
				LocalDate.now().plusDays(4), "Personal", BigDecimal.valueOf(2));
		when(leaveRequestRepository.findByPublicIdAndTenantId(requestPublicId, 1L)).thenReturn(Optional.of(request));
		when(employeeRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L))
				.thenReturn(Optional.of(employeeWithId(10L)));
		when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

		LeaveRequest cancelled = assertDoesNotThrow(() -> leaveRequestService.cancel(requestPublicId.toString()));

		assertEquals(LeaveRequestStatus.CANCELLED, cancelled.getStatus());
	}

	@Test
	void cancel_byNonOwningEmployee_throwsAccessDenied() {
		UUID requestPublicId = UUID.randomUUID();
		authenticateAsUser(CURRENT_USER_ID);
		LeaveRequest request = LeaveRequest.submit(999L, 20L, 30L, LocalDate.now().plusDays(3),
				LocalDate.now().plusDays(4), "Personal", BigDecimal.valueOf(2));
		when(leaveRequestRepository.findByPublicIdAndTenantId(requestPublicId, 1L)).thenReturn(Optional.of(request));
		when(employeeRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L))
				.thenReturn(Optional.of(employeeWithId(10L)));

		assertThrows(AccessDeniedException.class, () -> leaveRequestService.cancel(requestPublicId.toString()));
		verify(leaveRequestRepository, never()).save(any());
	}

	@Test
	void cancel_previouslyApprovedRequest_creditsBackBalance() {
		UUID requestPublicId = UUID.randomUUID();
		authenticateAsUser(CURRENT_USER_ID);
		LeaveRequest request = LeaveRequest.submit(10L, 20L, 30L, LocalDate.now().plusDays(3),
				LocalDate.now().plusDays(4), "Personal", BigDecimal.valueOf(2));
		request.approve(1L);
		LeaveBalance balance = LeaveBalance.allocate(10L, 20L, 30L, BigDecimal.TEN);
		balance.deduct(request.getDaysRequested());
		when(leaveRequestRepository.findByPublicIdAndTenantId(requestPublicId, 1L)).thenReturn(Optional.of(request));
		when(employeeRepository.findByUserIdAndTenantId(CURRENT_USER_ID, 1L))
				.thenReturn(Optional.of(employeeWithId(10L)));
		when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(10L, 20L, 30L, 1L))
				.thenReturn(Optional.of(balance));
		when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

		leaveRequestService.cancel(requestPublicId.toString());

		assertEquals(0, BigDecimal.TEN.compareTo(balance.remainingDays()));
	}
}
