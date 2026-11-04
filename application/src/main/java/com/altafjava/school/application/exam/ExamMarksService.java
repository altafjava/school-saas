package com.altafjava.school.application.exam;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.error.AcademicErrorCode;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import lombok.RequiredArgsConstructor;

/**
 * Moves an exam's marks through entered, verified and locked. Verification is a second pair of
 * eyes, so nobody verifies marks they entered themselves. Every step is audited.
 */
@Service
@RequiredArgsConstructor
public class ExamMarksService {

	private final ExamRepository examRepository;
	private final GradeRepository gradeRepository;

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Exam", details = "Exam marks verified")
	public Exam verify(String examPublicId, String verifiedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Exam exam = require(tenantId, examPublicId);
		if (!gradeRepository.existsByExamIdAndTenantId(exam.getId(), tenantId)) {
			throw new BusinessException(AcademicErrorCode.EXAM_HAS_NO_MARKS,
					"An exam with no marks recorded cannot be verified");
		}
		if (gradeRepository.existsByExamIdAndGradedByAndTenantId(exam.getId(), verifiedBy, tenantId)) {
			throw new AccessDeniedException("Marks must be verified by someone who did not enter them");
		}
		exam.verifyMarks(verifiedBy);
		return examRepository.save(exam);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Exam", details = "Exam marks returned for correction")
	public Exam returnForCorrection(String examPublicId) {
		Exam exam = require(TenantContext.getCurrentTenantId(), examPublicId);
		exam.returnMarksForCorrection();
		return examRepository.save(exam);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Exam", details = "Exam marks locked")
	public Exam lock(String examPublicId, String lockedBy) {
		Exam exam = require(TenantContext.getCurrentTenantId(), examPublicId);
		exam.lockMarks(lockedBy);
		return examRepository.save(exam);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Exam", details = "Exam marks unlocked")
	public Exam unlock(String examPublicId) {
		Exam exam = require(TenantContext.getCurrentTenantId(), examPublicId);
		exam.unlockMarks();
		return examRepository.save(exam);
	}

	private Exam require(Long tenantId, String examPublicId) {
		return examRepository.findByPublicIdAndTenantId(UUID.fromString(examPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examPublicId));
	}
}
