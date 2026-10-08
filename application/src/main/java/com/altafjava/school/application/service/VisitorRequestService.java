package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.security.VisitorRequestAuthorizer;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.visitor.model.VisitorRequest;
import com.altafjava.school.domain.visitor.model.VisitorRequestStatus;
import com.altafjava.school.domain.visitor.repository.VisitorRequestRepository;
import lombok.RequiredArgsConstructor;

/**
 * The approval workflow in front of the gate: a visit is requested, the host (or a security
 * approver) accepts it, and only then can the front desk check the visitor in.
 */
@Service
@RequiredArgsConstructor
public class VisitorRequestService {

	private static final LocalDate EARLIEST = LocalDate.of(1970, 1, 1);
	private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

	private final VisitorRequestRepository visitorRequestRepository;
	private final EmployeeRepository employeeRepository;
	private final VisitorRequestAuthorizer authorizer;
	private final NotificationService notificationService;

	@Transactional(readOnly = true)
	public Page<VisitorRequest> list(VisitorRequestStatus status, LocalDate from, LocalDate to, Pageable pageable) {
		return visitorRequestRepository.search(TenantContext.getCurrentTenantId(), from == null ? EARLIEST : from,
				to == null ? LATEST : to, status, pageable);
	}

	@Transactional(readOnly = true)
	public Page<VisitorRequest> listHostedByCurrentEmployee(Long actorUserId, VisitorRequestStatus status,
			LocalDate from, LocalDate to, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee host = employeeRepository.findByUserIdAndTenantId(actorUserId, tenantId)
				.orElseThrow(() -> new AccessDeniedException("No employee record linked to the current user"));
		return visitorRequestRepository.searchByHost(tenantId, host.getId(), from == null ? EARLIEST : from,
				to == null ? LATEST : to, status, pageable);
	}

	@Transactional(readOnly = true)
	public VisitorRequest findByPublicId(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		VisitorRequest request = require(tenantId, publicId);
		authorizer.assertMayManage(tenantId, request, actorUserId);
		return request;
	}

	/** A null {@code visitDate} raises a walk-in for today; a date pre-registers the visit. */
	@Transactional
	public VisitorRequest raise(String visitorName, String visitorPhone, String purpose, String hostPublicId,
			LocalDate visitDate, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee host = employeeRepository.findByPublicIdAndTenantId(UUID.fromString(hostPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Host not found: " + hostPublicId));
		if (!host.isActive()) {
			throw new BusinessException("The host has left the school");
		}
		authorizer.assertMayRaiseFor(tenantId, host, actorUserId);
		LocalDate today = LocalDate.now();
		VisitorRequest request = visitDate == null
				? VisitorRequest.walkIn(visitorName, visitorPhone, purpose, host.getId(), actorUserId, today)
				: VisitorRequest.preRegister(visitorName, visitorPhone, purpose, host.getId(), visitDate,
						actorUserId, today);
		VisitorRequest saved = visitorRequestRepository.save(request);
		notifyHost(tenantId, host, saved);
		return saved;
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "VisitorRequest", details = "Visit approved")
	public VisitorRequest approve(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		VisitorRequest request = require(tenantId, publicId);
		authorizer.assertMayDecide(tenantId, request, actorUserId);
		request.approve(actorUserId, LocalDate.now());
		return visitorRequestRepository.save(request);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "VisitorRequest", details = "Visit rejected")
	public VisitorRequest reject(String publicId, String reason, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		VisitorRequest request = require(tenantId, publicId);
		authorizer.assertMayDecide(tenantId, request, actorUserId);
		request.reject(actorUserId, reason);
		return visitorRequestRepository.save(request);
	}

	@Transactional
	public VisitorRequest cancel(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		VisitorRequest request = require(tenantId, publicId);
		authorizer.assertMayManage(tenantId, request, actorUserId);
		request.cancel();
		return visitorRequestRepository.save(request);
	}

	@Transactional
	public VisitorRequest attachPhoto(String publicId, String photoFilePublicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		VisitorRequest request = require(tenantId, publicId);
		authorizer.assertMayManage(tenantId, request, actorUserId);
		request.attachPhoto(UUID.fromString(photoFilePublicId));
		return visitorRequestRepository.save(request);
	}

	private VisitorRequest require(Long tenantId, String publicId) {
		return visitorRequestRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Visitor request not found: " + publicId));
	}

	private void notifyHost(Long tenantId, Employee host, VisitorRequest request) {
		if (host.getUserId() == null) {
			return;
		}
		notificationService.send(SendNotificationCommand.builder()
				.tenantId(tenantId)
				.userId(host.getUserId())
				.type(NotificationType.ANNOUNCEMENT)
				.title("Visitor awaiting your approval")
				.message(request.getVisitorName() + " is due on " + request.getVisitDate() + ": "
						+ request.getPurpose())
				.templateVariables(Map.of("visitDate", request.getVisitDate().toString()))
				.priority(NotificationPriority.NORMAL)
				.build());
	}
}
