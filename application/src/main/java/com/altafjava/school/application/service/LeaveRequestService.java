package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.altafjava.school.application.filter.LeaveRequestFilter;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.application.security.LeaveApprovalAuthorizer;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveApproval;
import com.altafjava.school.domain.leave.model.LeaveApprovalStage;
import com.altafjava.school.domain.leave.model.LeaveBalance;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveApprovalRepository;
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
	private final LeaveApprovalRepository leaveApprovalRepository;
	private final DepartmentRepository departmentRepository;
	private final LeaveApprovalAuthorizer leaveApprovalAuthorizer;
	private final LeaveDayCalculator leaveDayCalculator = new LeaveDayCalculator();
	private final PublicIdLookup publicIdLookup;

	public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, LeaveTypeRepository leaveTypeRepository,
			LeaveBalanceRepository leaveBalanceRepository, EmployeeRepository employeeRepository,
			AcademicYearRepository academicYearRepository, TenantAdminNotifier tenantAdminNotifier,
			NotificationService notificationService, HolidayService holidayService,
			LeaveApprovalRepository leaveApprovalRepository, DepartmentRepository departmentRepository,
			LeaveApprovalAuthorizer leaveApprovalAuthorizer, PublicIdLookup publicIdLookup) {
		this.publicIdLookup = publicIdLookup;
		this.leaveRequestRepository = leaveRequestRepository;
		this.leaveTypeRepository = leaveTypeRepository;
		this.leaveBalanceRepository = leaveBalanceRepository;
		this.employeeRepository = employeeRepository;
		this.academicYearRepository = academicYearRepository;
		this.tenantAdminNotifier = tenantAdminNotifier;
		this.notificationService = notificationService;
		this.holidayService = holidayService;
		this.leaveApprovalRepository = leaveApprovalRepository;
		this.departmentRepository = departmentRepository;
		this.leaveApprovalAuthorizer = leaveApprovalAuthorizer;
	}

	@Transactional(readOnly = true)
	public Page<LeaveRequest> listForCurrentEmployee(LeaveRequestFilter filter, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = resolveCurrentEmployee(tenantId);
		return search(tenantId, employee.getId(), filter, pageable);
	}

	@Transactional(readOnly = true)
	public Page<LeaveRequest> listAll(LeaveRequestFilter filter, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return search(tenantId, publicIdLookup.idOrNull(EntityRef.EMPLOYEE, filter.employeePublicId()), filter,
				pageable);
	}

	private Page<LeaveRequest> search(Long tenantId, Long employeeId, LeaveRequestFilter filter,
			Pageable pageable) {
		return leaveRequestRepository.search(tenantId, employeeId,
				publicIdLookup.idOrNull(EntityRef.LEAVE_TYPE, filter.leaveTypePublicId()), filter.status(),
				filter.dates().from(), filter.dates().to(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<LeaveRequest> listAwaitingMyReview(Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee head = resolveCurrentEmployee(tenantId);
		List<Long> departmentIds = departmentRepository.findIdsHeadedBy(tenantId, head.getId());
		if (departmentIds.isEmpty()) {
			return Page.empty(pageable);
		}
		List<Long> employeeIds = employeeRepository.findIdsInDepartments(tenantId, departmentIds);
		if (employeeIds.isEmpty()) {
			return Page.empty(pageable);
		}
		return leaveRequestRepository.findAwaitingDepartmentHead(tenantId, employeeIds, pageable);
	}

	@Transactional(readOnly = true)
	public List<LeaveApproval> listApprovals(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LeaveRequest request = findRequest(tenantId, publicId);
		return leaveApprovalRepository.findAllByLeaveRequestIdAndTenantIdOrderByDecidedAtAsc(request.getId(),
				tenantId);
	}

	@Transactional
	public LeaveRequest submit(String leaveTypePublicId, LocalDate startDate, LocalDate endDate, String reason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = resolveCurrentEmployee(tenantId);
		LeaveType leaveType = findLeaveType(tenantId, leaveTypePublicId);
		AcademicYear academicYear = academicYearRepository.findByCurrentTrueAndTenantId(tenantId)
				.orElseThrow(() -> new BusinessException("No current academic year configured for this tenant"));
		if (!leaveType.isActive()) {
			throw new BusinessException("Leave type '" + leaveType.getName() + "' is no longer available");
		}
		if (employee.isOnProbation(LocalDate.now()) && !leaveType.isAvailableDuringProbation()) {
			throw new BusinessException(
					"Leave type '" + leaveType.getName() + "' is not available during probation");
		}
		if (leaveRequestRepository.existsOverlapping(tenantId, employee.getId(), startDate, endDate)) {
			throw new BusinessException("You already have a leave request covering some of these days");
		}

		Set<LocalDate> holidayDates = holidayService.datesInRange(tenantId, startDate, endDate);
		BigDecimal daysRequested = leaveDayCalculator.calculateDays(startDate, endDate, holidayDates);

		leaveBalanceRepository
				.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(employee.getId(), leaveType.getId(),
						academicYear.getId(), tenantId)
				.ifPresent(balance -> validateSufficientBalance(balance, daysRequested));

		Optional<Employee> departmentHead = leaveType.requiresDepartmentHeadApproval()
				? findActionableDepartmentHead(tenantId, employee)
				: Optional.empty();
		LeaveRequest request = LeaveRequest.submit(employee.getId(), leaveType.getId(), academicYear.getId(),
				startDate, endDate, reason, daysRequested, departmentHead.isPresent() ? 2 : 1);
		LeaveRequest saved = leaveRequestRepository.save(request);
		departmentHead.ifPresentOrElse(
				head -> notifyDepartmentHeadOfRequest(tenantId, head, employee, leaveType, saved),
				() -> notifyAdminsOfRequest(tenantId, employee, leaveType, saved));
		return saved;
	}

	@Transactional
	public LeaveRequest approve(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LeaveRequest request = findRequest(tenantId, publicId);
		Long actorUserId = resolveCurrentUserId();
		LeaveApprovalStage stage = authorizeDecision(tenantId, request, actorUserId, "approve");
		if (leaveApprovalRepository.existsByLeaveRequestIdAndDecidedByUserIdAndTenantId(request.getId(), actorUserId,
				tenantId)) {
			throw new BusinessException("Each level of approval must come from a different person");
		}
		LeaveBalance balance = leaveBalanceRepository
				.findByEmployeeIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(request.getEmployeeId(),
						request.getLeaveTypeId(), request.getAcademicYearId(), tenantId)
				.orElseThrow(() -> new BusinessException(
						"No leave balance allocated for employee " + request.getEmployeeId()));
		leaveApprovalRepository.save(LeaveApproval.approved(request.getId(), stage, actorUserId));
		request.approve(actorUserId);
		if (request.getStatus() == LeaveRequestStatus.APPROVED) {
			balance.deduct(request.getDaysRequested());
			leaveBalanceRepository.save(balance);
		}
		LeaveRequest saved = leaveRequestRepository.save(request);
		if (saved.getStatus() == LeaveRequestStatus.APPROVED) {
			notifyEmployeeOfDecision(tenantId, saved, NotificationType.LEAVE_APPROVED,
					"Your leave request was approved");
		} else {
			notifyAdminsAwaitingFinalApproval(tenantId, saved);
		}
		return saved;
	}

	@Transactional
	public LeaveRequest reject(String publicId, String rejectionReason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LeaveRequest request = findRequest(tenantId, publicId);
		Long actorUserId = resolveCurrentUserId();
		LeaveApprovalStage stage = authorizeDecision(tenantId, request, actorUserId, "reject");
		leaveApprovalRepository.save(LeaveApproval.rejected(request.getId(), stage, actorUserId, rejectionReason));
		request.reject(actorUserId, rejectionReason);
		LeaveRequest saved = leaveRequestRepository.save(request);
		notifyEmployeeOfDecision(tenantId, saved, NotificationType.LEAVE_REJECTED, "Your leave request was rejected");
		return saved;
	}

	private LeaveApprovalStage authorizeDecision(Long tenantId, LeaveRequest request, Long actorUserId,
			String action) {
		LeaveApprovalStage stage = request.awaitingStage().orElseThrow(
				() -> new BusinessException("Cannot " + action + " a leave request in status " + request.getStatus()));
		leaveApprovalAuthorizer.assertMayDecide(tenantId, request, stage, actorUserId);
		return stage;
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

	// The first approval needs someone who can act on it: an active head with a login, other than the requester.
	private Optional<Employee> findActionableDepartmentHead(Long tenantId, Employee requester) {
		if (requester.getDepartmentId() == null) {
			return Optional.empty();
		}
		return departmentRepository.findByIdAndTenantId(requester.getDepartmentId(), tenantId)
				.map(Department::getHeadEmployeeId)
				.flatMap(headId -> employeeRepository.findByIdAndTenantId(headId, tenantId))
				.filter(Employee::isActive)
				.filter(head -> head.getUserId() != null)
				.filter(head -> !head.getId().equals(requester.getId()));
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

	private void notifyDepartmentHeadOfRequest(Long tenantId, Employee head, Employee employee, LeaveType leaveType,
			LeaveRequest request) {
		String employeeName = employee.getFirstName() + " " + employee.getLastName();
		notificationService.send(SendNotificationCommand.builder()
				.tenantId(tenantId)
				.userId(head.getUserId())
				.type(NotificationType.LEAVE_REQUESTED)
				.title("Leave Request: " + employeeName)
				.message(employeeName + " requested " + request.getDaysRequested() + " day(s) of "
						+ leaveType.getName() + " and needs your approval")
				.templateVariables(Map.of(
						"employeeName", employeeName,
						"leaveTypeName", leaveType.getName(),
						"startDate", request.getStartDate().toString(),
						"endDate", request.getEndDate().toString(),
						"daysRequested", request.getDaysRequested().toString()))
				.priority(NotificationPriority.NORMAL)
				.build());
	}

	private void notifyAdminsAwaitingFinalApproval(Long tenantId, LeaveRequest request) {
		employeeRepository.findByIdAndTenantId(request.getEmployeeId(), tenantId).ifPresent(employee -> {
			String employeeName = employee.getFirstName() + " " + employee.getLastName();
			tenantAdminNotifier.notifyAll(tenantId, NotificationType.LEAVE_REQUESTED,
					"Leave Request awaiting final approval: " + employeeName,
					"The department head approved " + employeeName + "'s request for "
							+ request.getDaysRequested() + " day(s)",
					Map.of(
							"employeeName", employeeName,
							"startDate", request.getStartDate().toString(),
							"endDate", request.getEndDate().toString(),
							"daysRequested", request.getDaysRequested().toString()));
		});
	}
}
