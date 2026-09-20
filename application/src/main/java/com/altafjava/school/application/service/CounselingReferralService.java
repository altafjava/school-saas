package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.counseling.model.CounselingReferral;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.counseling.repository.CounselingReferralRepository;
import com.altafjava.school.domain.counseling.repository.CounselingSessionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class CounselingReferralService {

	private final CounselingReferralRepository counselingReferralRepository;
	private final CounselingSessionRepository counselingSessionRepository;
	private final StudentRepository studentRepository;
	private final ActivityLogService activityLogService;

	public CounselingReferralService(CounselingReferralRepository counselingReferralRepository,
			CounselingSessionRepository counselingSessionRepository, StudentRepository studentRepository,
			ActivityLogService activityLogService) {
		this.counselingReferralRepository = counselingReferralRepository;
		this.counselingSessionRepository = counselingSessionRepository;
		this.studentRepository = studentRepository;
		this.activityLogService = activityLogService;
	}

	@Transactional(readOnly = true)
	public Page<CounselingReferral> listAll(Pageable pageable) {
		return counselingReferralRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<CounselingReferral> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return counselingReferralRepository.findAllByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public CounselingReferral get(String publicId) {
		return findByPublicId(publicId);
	}

	@Transactional
	public CounselingReferral refer(String studentPublicId, String reason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));

		CounselingReferral referral = CounselingReferral.refer(student.getId(), resolveCurrentUserId(), reason);
		CounselingReferral saved = counselingReferralRepository.save(referral);
		// reason is deliberately excluded from the audit trail — confidential referral content.
		logAction(tenantId, "CREATE", String.valueOf(saved.getId()), "Counseling referral created");
		return saved;
	}

	private void logAction(Long tenantId, String action, String resourceId, String details) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String actorId = authentication != null ? authentication.getName() : "system";
		activityLogService.log(tenantId, action, "CounselingReferral", resourceId, actorId, null, null, details,
				null, null);
	}

	@Transactional
	public CounselingReferral scheduleWithSession(String publicId, String counselingSessionPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		CounselingReferral referral = findByPublicId(publicId);
		CounselingSession session = counselingSessionRepository
				.findByPublicIdAndTenantId(UUID.fromString(counselingSessionPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Counseling session not found: " + counselingSessionPublicId));
		if (!session.getStudentId().equals(referral.getStudentId())) {
			throw new BusinessException("Counseling session must belong to the same student as the referral");
		}

		referral.scheduleWithSession(session.getId());
		CounselingReferral saved = counselingReferralRepository.save(referral);
		logAction(tenantId, "UPDATE", publicId, "Counseling referral scheduled with session");
		return saved;
	}

	@Transactional
	public CounselingReferral complete(String publicId) {
		CounselingReferral referral = findByPublicId(publicId);
		referral.complete();
		CounselingReferral saved = counselingReferralRepository.save(referral);
		logAction(TenantContext.getCurrentTenantId(), "UPDATE", publicId, "Counseling referral completed");
		return saved;
	}

	@Transactional
	public CounselingReferral decline(String publicId) {
		CounselingReferral referral = findByPublicId(publicId);
		referral.decline();
		CounselingReferral saved = counselingReferralRepository.save(referral);
		logAction(TenantContext.getCurrentTenantId(), "UPDATE", publicId, "Counseling referral declined");
		return saved;
	}

	private CounselingReferral findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return counselingReferralRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Counseling referral not found: " + publicId));
	}

	private Long resolveCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot resolve referring user");
	}
}
