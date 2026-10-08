package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.visitor.VisitorBadgeIssuer;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.visitor.model.VisitorLog;
import com.altafjava.school.domain.visitor.model.VisitorRequest;
import com.altafjava.school.domain.visitor.repository.VisitorLogRepository;
import com.altafjava.school.domain.visitor.repository.VisitorRequestRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Lets visitors in and out. A visitor is admitted only against an approved request for today, with
 * a photo on record, and leaves with a badge that stops being valid when they check out. Like
 * report cards, the badge is rendered and stored outside any database transaction; only the
 * check-in record itself is written in a short one.
 */
@Slf4j
@Service
public class VisitorLogService {

	private final VisitorLogRepository visitorLogRepository;
	private final VisitorRequestRepository visitorRequestRepository;
	private final EmployeeRepository employeeRepository;
	private final VisitorBadgeIssuer visitorBadgeIssuer;
	private final DocumentIssuanceService documentIssuanceService;
	private final TransactionTemplate transactionTemplate;

	public VisitorLogService(VisitorLogRepository visitorLogRepository,
			VisitorRequestRepository visitorRequestRepository, EmployeeRepository employeeRepository,
			VisitorBadgeIssuer visitorBadgeIssuer, DocumentIssuanceService documentIssuanceService,
			PlatformTransactionManager transactionManager) {
		this.visitorLogRepository = visitorLogRepository;
		this.visitorRequestRepository = visitorRequestRepository;
		this.employeeRepository = employeeRepository;
		this.visitorBadgeIssuer = visitorBadgeIssuer;
		this.documentIssuanceService = documentIssuanceService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Transactional(readOnly = true)
	public Page<VisitorLog> list(Boolean stillCheckedIn, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (Boolean.TRUE.equals(stillCheckedIn)) {
			return visitorLogRepository.findAllByTenantIdAndCheckOutAtIsNull(tenantId, pageable);
		}
		if (from != null && to != null) {
			return visitorLogRepository.findAllByTenantIdAndCheckInAtBetween(tenantId, from, to, pageable);
		}
		return visitorLogRepository.findAllByTenantId(tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public VisitorLog findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return visitorLogRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Visitor log not found: " + publicId));
	}

	/**
	 * @param photoFilePublicId
	 *                              the photo taken at the gate, or null to use the one already on the request
	 */
	@Audited(action = AuditAction.CREATE, resourceType = "VisitorLog", details = "Visitor checked in")
	public VisitorLog checkIn(String requestPublicId, String photoFilePublicId, Long checkedInByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LocalDate today = LocalDate.now();
		AdmittedVisit visit = transactionTemplate
				.execute(status -> admit(tenantId, requestPublicId, photoFilePublicId, today));
		DocumentIssuance badge = visitorBadgeIssuer.issue(tenantId, visit.request(), visit.host(), visit.photo(),
				today, checkedInByUserId);
		try {
			return transactionTemplate.execute(status -> recordCheckIn(tenantId, visit, badge));
		} catch (RuntimeException ex) {
			log.error("action=visitor-check-in-failed tenantId={} requestId={} badgeId={} — revoking unused badge",
					tenantId, visit.request().getId(), badge.getId(), ex);
			revokeQuietly(badge, "Check-in could not be saved");
			throw ex;
		}
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "VisitorLog", details = "Visitor checked out")
	public VisitorLog checkOut(String publicId) {
		VisitorLog visitorLog = findByPublicId(publicId);
		visitorLog.checkOut(LocalDateTime.now());
		VisitorLog saved = visitorLogRepository.save(visitorLog);
		if (saved.getBadgeIssuanceId() != null) {
			documentIssuanceService.findById(saved.getBadgeIssuanceId())
					.filter(badge -> !badge.isRevoked())
					.ifPresent(badge -> revokeQuietly(badge, "Visitor checked out"));
		}
		return saved;
	}

	@Transactional(readOnly = true)
	public DocumentIssuance findBadge(String publicId) {
		VisitorLog visitorLog = findByPublicId(publicId);
		if (visitorLog.getBadgeIssuanceId() == null) {
			throw new ResourceNotFoundException("No badge was issued for this visit");
		}
		return documentIssuanceService.findById(visitorLog.getBadgeIssuanceId())
				.orElseThrow(() -> new ResourceNotFoundException("No badge was issued for this visit"));
	}

	public byte[] downloadBadgePdf(DocumentIssuance badge) {
		return documentIssuanceService.downloadPdf(badge);
	}

	private AdmittedVisit admit(Long tenantId, String requestPublicId, String photoFilePublicId, LocalDate today) {
		VisitorRequest request = visitorRequestRepository
				.findByPublicIdAndTenantId(UUID.fromString(requestPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Visitor request not found: " + requestPublicId));
		request.requireAdmissibleOn(today);
		UUID photo = photoFilePublicId != null ? UUID.fromString(photoFilePublicId) : request.getPhotoFilePublicId();
		if (photo == null) {
			throw new BusinessException("A photo of the visitor is required to check them in");
		}
		Employee host = employeeRepository.findByIdAndTenantId(request.getHostEmployeeId(), tenantId)
				.filter(Employee::isActive)
				.orElseThrow(() -> new BusinessException("The host is no longer at the school"));
		return new AdmittedVisit(request, host, photo);
	}

	private VisitorLog recordCheckIn(Long tenantId, AdmittedVisit visit, DocumentIssuance badge) {
		VisitorRequest request = visitorRequestRepository.findByIdAndTenantId(visit.request().getId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Visitor request not found"));
		VisitorLog visitorLog = VisitorLog.checkIn(request, visit.photo(), LocalDateTime.now());
		visitorLog.attachBadge(badge.getId());
		request.markCheckedIn();
		visitorRequestRepository.save(request);
		return visitorLogRepository.save(visitorLog);
	}

	private void revokeQuietly(DocumentIssuance badge, String reason) {
		try {
			documentIssuanceService.revoke(badge, reason);
		} catch (RuntimeException ex) {
			log.error("action=visitor-badge-revoke-failed badgeId={}", badge.getId(), ex);
		}
	}

	private record AdmittedVisit(VisitorRequest request, Employee host, UUID photo) {
	}
}
