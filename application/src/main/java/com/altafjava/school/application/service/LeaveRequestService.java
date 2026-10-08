package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveBalance;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveBalanceRepository;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;
import com.altafjava.school.domain.leave.service.LeaveDayCalculator;

@Service
public class LeaveRequestService {

	private final LeaveRequestRepository leaveRequestRepository;
	private final LeaveTypeRepository leaveTypeRepository;
	private final LeaveBalanceRepository leaveBalanceRepository;
	private final EmployeeRepository employeeRepository;
	private final AcademicYearRepository academicYearRepository;
	private final TenantAdminNotifier tenantAdminNotifier;
	private final NotificationService notificationService;
	private final HolidayService holidayService;
	private final LeaveDayCalculator leaveDayCalculator = new LeaveDayCalculator();

	public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, LeaveTypeRepository leaveTypeRepository,
			LeaveBalanceRepository leaveBalanceRepository, EmployeeRepository employeeRepository,
			AcademicYearRepository academicYearRepository, TenantAdminNotifier tenantAdminNotifier,
			NotificationService notificationService, HolidayService holidayService) {
		this.leaveRequestRepository = leaveRequestRepository;
		this.leaveTypeRepository = leaveTypeRepository;
		this.leaveBalanceRepository = leaveBalanceRepository;
		this.employeeRepository = employeeRepository;
		this.academicYearRepository = academicYearRepository;
		this.tenantAdminNotifier = tenantAdminNotifier;
		this.notificationService = notificationService;
		this.holidayService = holidayService;
	}

	@Transactional(readOnly = true)
	public Page<LeaveRequest> listForCurrentEmployee(Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = resolveCurrentEmployee(tenantId);
		return leaveRequestRepository.findAllByEmployeeIdAndTenantId(employee.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public Page<LeaveRequest> listAll(Pageable pageable) {
		return leaveRequestRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional
	public LeaveRequest submit(String leaveTypePublicId, LocalDate startDate, LocalDate endDate, String reason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = resolveCurrentEmployee(tenantId);
		LeaveType leaveType = findLeaveType(tenantId, leaveTypePublicId);
		AcademicYear academicYear = academicYearRepository.findByCurrentTrueAndTenantId(tenantId)
				.orElseThrow(() -> new BusinessException("No current academic year configured for this tenant"));
		if (employee.isOnProbation(LocalDate.now()) && !leaveType.isAvailableDuringProbation()) {
			throw new BusinessException(
					"Leave type '" + leaveType.getName() + "' is not available during probation");
		}

		Set<LocalDate> holidayDates = holidayService.datesInRange(tenantId, startDate, endDate);
		BigDecimal daysRequested = leaveDayCalculator.calculateDays(startDate, endDate, holidayDates);

		leaveBalanceRepository
				.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(employee.getId(), leaveType.getId(),
						academicYear.getId(), tenantId)
				.ifPresent(balance -> validateSufficientBalance(balance, daysRequested));

		LeaveRequest request = LeaveRequest.submit(employee.getId(), leaveType.getId(), academicYear.getId(),
				startDate, endDate, reason, daysRequested);
		LeaveRequest saved = leaveRequestRepository.save(request);
		notifyAdminsOfRequest(tenantId, employee, leaveType, saved);
		return saved;
	}

	@Transactional
	public LeaveRequest approve(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LeaveRequest request = findRequest(tenantId, publicId);
		LeaveBalance balance = leaveBalanceRepository
				.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(request.getEmployeeId(),
						request.getLeaveTypeId(), request.getAcademicYearId(), tenantId)
				.orElseThrow(() -> new BusinessException(
						"No leave balance allocated for employee " + request.getEmployeeId()));
		balance.deduct(request.getDaysRequested());
		request.approve(resolveCurrentUserId());
		leaveBalanceRepository.save(balance);
		LeaveRequest saved = leaveRequestRepository.save(request);
		notifyEmployeeOfDecision(tenantId, saved, NotificationType.LEAVE_APPROVED, "Your leave request was approved");
		return saved;
	}

	@Transactional
	public LeaveRequest reject(String publicId, String rejectionReason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LeaveRequest request = findRequest(tenantId, publicId);
		request.reject(resolveCurrentUserId(), rejectionReason);
		LeaveRequest saved = leaveRequestRepository.save(request);
		notifyEmployeeOfDecision(tenantId, saved, NotificationType.LEAVE_REJECTED, "Your leave request was rejected");
		return saved;
	}

	@Transactional
	public LeaveRequest cancel(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LeaveRequest request = findRequest(tenantId, publicId);
		Employee currentEmployee = resolveCurrentEmployee(tenantId);
		if (!request.getEmployeeId().equals(currentEmployee.getId())) {
			throw new AccessDeniedException("Cannot cancel another employee's leave request");
		}
		boolean wasApproved = request.getStatus() == LeaveRequestStatus.APPROVED;
		request.cancel();
		if (wasApproved) {
			leaveBalanceRepository
					.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(request.getEmployeeId(),
							request.getLeaveTypeId(), request.getAcademicYearId(), tenantId)
					.ifPresent(balance -> {
						balance.credit(request.getDaysRequested());
						leaveBalanceRepository.save(balance);
					});
		}
		return leaveRequestRepository.save(request);
	}

	private void validateSufficientBalance(LeaveBalance balance, BigDecimal daysRequested) {
		if (daysRequested.compareTo(balance.remainingDays()) > 0) {
			throw new BusinessException(
					"Insufficient leave balance: requested " + daysRequested + ", remaining "
							+ balance.remainingDays());
		}
	}

	private LeaveType findLeaveType(Long tenantId, String publicId) {
		return leaveTypeRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave type not found: " + publicId));
	}

	private LeaveRequest findRequest(Long tenantId, String publicId) {
		return leaveRequestRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + publicId));
	}

	private Employee resolveCurrentEmployee(Long tenantId) {
		Long userId = resolveCurrentUserId();
		return employeeRepository.findByUserIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new AccessDeniedException("No employee record linked to the current user"));
	}

	private Long resolveCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot resolve leave request actor");
	}

	private void notifyAdminsOfRequest(Long tenantId, Employee employee, LeaveType leaveType, LeaveRequest request) {
		String employeeName = employee.getFirstName() + " " + employee.getLastName();
		tenantAdminNotifier.notifyAll(tenantId, NotificationType.LEAVE_REQUESTED,
				"Leave Request: " + employeeName,
				employeeName + " requested " + request.getDaysRequested() + " day(s) of " + leaveType.getName(),
				Map.of(
						"employeeName", employeeName,
						"leaveTypeName", leaveType.getName(),
						"startDate", request.getStartDate().toString(),
						"endDate", request.getEndDate().toString(),
						"daysRequested", request.getDaysRequested().toString()));
	}

	private void notifyEmployeeOfDecision(Long tenantId, LeaveRequest request, NotificationType type, String message) {
		employeeRepository.findByIdAndTenantId(request.getEmployeeId(), tenantId)
				.map(Employee::getUserId)
				.ifPresent(userId -> notificationService.send(SendNotificationCommand.builder()
						.tenantId(tenantId)
						.userId(userId)
						.type(type)
						.title("Leave Request Update")
						.message(message)
						.templateVariables(Map.of(
								"startDate", request.getStartDate().toString(),
								"endDate", request.getEndDate().toString(),
								"daysRequested", request.getDaysRequested().toString(),
								"rejectionReason", request.getRejectionReason() != null
										? request.getRejectionReason()
										: ""))
						.priority(NotificationPriority.NORMAL)
						.build()));
	}
}
