package com.altafjava.school.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.GuardianConsentRecord;
import com.altafjava.school.domain.guardian.model.GuardianConsentType;
import com.altafjava.school.domain.guardian.repository.GuardianConsentRecordRepository;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * Self-service consent capture for the guardian's own linked student — the legally meaningful act
 * for FERPA/COPPA/GDPR-K/DPDP is the guardian granting consent themselves, not an admin recording
 * it on their behalf (see {@code GuardianService#grantConsent}, which is a distinct, admin-driven
 * link-confirmation action).
 */
@Service
public class GuardianConsentService {

	private final GuardianConsentRecordRepository consentRecordRepository;
	private final GuardianRepository guardianRepository;
	private final StudentRepository studentRepository;
	private final StudentGuardianLinkRepository studentGuardianLinkRepository;

	public GuardianConsentService(GuardianConsentRecordRepository consentRecordRepository,
			GuardianRepository guardianRepository, StudentRepository studentRepository,
			StudentGuardianLinkRepository studentGuardianLinkRepository) {
		this.consentRecordRepository = consentRecordRepository;
		this.guardianRepository = guardianRepository;
		this.studentRepository = studentRepository;
		this.studentGuardianLinkRepository = studentGuardianLinkRepository;
	}

	@Transactional
	public GuardianConsentRecord grant(String studentPublicId, GuardianConsentType consentType, String policyVersion) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Guardian guardian = currentGuardian(tenantId);
		Student student = requireLinkedStudent(tenantId, studentPublicId, guardian);

		Optional<GuardianConsentRecord> existing = consentRecordRepository
				.findByGuardianIdAndStudentIdAndConsentTypeAndTenantId(guardian.getId(), student.getId(), consentType,
						tenantId);
		if (existing.isPresent()) {
			return grantOnRecord(existing.get(), policyVersion);
		}

		GuardianConsentRecord record = GuardianConsentRecord.create(student.getId(), guardian.getId(), consentType);
		record.grant(policyVersion);
		record.setTenantId(tenantId);
		try {
			return consentRecordRepository.save(record);
		} catch (DataIntegrityViolationException e) {
			// Two concurrent grant calls for the same (guardian, student, type) can both miss the
			// existence check above and both attempt to insert — the uq_guardian_consent_records
			// unique index (035-retention-and-guardian-consent.xml) catches the race, and the
			// losing insert lands here. Retrying as an update on the winner's row (rather than
			// surfacing the raw constraint-violation exception) is safe within this same
			// transaction: MariaDB rolls back only the failed statement on a duplicate-key error,
			// not the whole transaction, so this SELECT/UPDATE can still run in it.
			GuardianConsentRecord raceWinner = consentRecordRepository
					.findByGuardianIdAndStudentIdAndConsentTypeAndTenantId(guardian.getId(), student.getId(),
							consentType, tenantId)
					.orElseThrow(() -> e);
			return grantOnRecord(raceWinner, policyVersion);
		}
	}

	private GuardianConsentRecord grantOnRecord(GuardianConsentRecord record, String policyVersion) {
		record.grant(policyVersion);
		return consentRecordRepository.save(record);
	}

	@Transactional
	public GuardianConsentRecord revoke(String studentPublicId, GuardianConsentType consentType) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Guardian guardian = currentGuardian(tenantId);
		Student student = requireLinkedStudent(tenantId, studentPublicId, guardian);

		GuardianConsentRecord record = consentRecordRepository
				.findByGuardianIdAndStudentIdAndConsentTypeAndTenantId(guardian.getId(), student.getId(), consentType,
						tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"No " + consentType + " consent record found for student " + studentPublicId));
		record.revoke();
		return consentRecordRepository.save(record);
	}

	@Transactional(readOnly = true)
	public List<GuardianConsentRecord> listMine(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Guardian guardian = currentGuardian(tenantId);
		Student student = requireLinkedStudent(tenantId, studentPublicId, guardian);
		return consentRecordRepository.findAllByGuardianIdAndStudentIdAndTenantId(guardian.getId(), student.getId(),
				tenantId);
	}

	@Transactional(readOnly = true)
	public List<GuardianConsentRecord> listForStudent(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return consentRecordRepository.findAllByStudentIdAndTenantId(student.getId(), tenantId);
	}

	private Student requireLinkedStudent(Long tenantId, String studentPublicId, Guardian guardian) {
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		if (!studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(guardian.getId(),
				student.getId(), tenantId)) {
			throw new AccessDeniedException(
					"The current guardian is not linked to student " + studentPublicId);
		}
		return student;
	}

	private Guardian currentGuardian(Long tenantId) {
		Long userId = currentUserId();
		return guardianRepository.findByUserIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("No guardian record linked to the current user"));
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new AccessDeniedException("Authenticated principal missing");
	}
}
