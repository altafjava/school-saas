package com.altafjava.school.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.GuardianAuthorizationChange;
import com.altafjava.school.domain.guardian.model.PickupDecision;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianAuthorizationChangeRepository;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Answers "is this adult allowed to take this child home" and owns every change to the answer.
 * Every mutation is both {@code @Audited} and written to the append-only
 * {@link GuardianAuthorizationChange} history. Lookups fail closed: no link means
 * {@link PickupDecision#NOT_LINKED}, never an implicit yes.
 */
@Service
@RequiredArgsConstructor
public class GuardianPickupService {

	public record PickupAuthorizedGuardian(StudentGuardianLink link, Guardian guardian) {
	}

	private final GuardianRepository guardianRepository;
	private final StudentRepository studentRepository;
	private final StudentGuardianLinkRepository linkRepository;
	private final GuardianAuthorizationChangeRepository changeRepository;

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "StudentGuardianLink", details = "Pickup authorized")
	public StudentGuardianLink authorizePickup(String guardianPublicId, String studentPublicId, Long changedByUserId) {
		return change(guardianPublicId, studentPublicId, changedByUserId, null, StudentGuardianLink::authorizePickup);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "StudentGuardianLink", details = "Pickup authorization revoked")
	public StudentGuardianLink revokePickup(String guardianPublicId, String studentPublicId, Long changedByUserId) {
		return change(guardianPublicId, studentPublicId, changedByUserId, null,
				StudentGuardianLink::revokePickupAuthorization);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "StudentGuardianLink", details = "Custody restricted")
	public StudentGuardianLink restrictCustody(String guardianPublicId, String studentPublicId, String note,
			Long changedByUserId) {
		return change(guardianPublicId, studentPublicId, changedByUserId, note, link -> link.restrictCustody(note));
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "StudentGuardianLink", details = "Custody restriction lifted")
	public StudentGuardianLink liftCustodyRestriction(String guardianPublicId, String studentPublicId,
			Long changedByUserId) {
		return change(guardianPublicId, studentPublicId, changedByUserId, null,
				StudentGuardianLink::liftCustodyRestriction);
	}

	@Transactional(readOnly = true)
	public PickupDecision check(String studentPublicId, String guardianPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		Optional<Guardian> guardian = guardianRepository.findByPublicIdAndTenantId(UUID.fromString(guardianPublicId),
				tenantId);
		if (guardian.isEmpty()) {
			return PickupDecision.NOT_LINKED;
		}
		return linkRepository.findByGuardianIdAndStudentIdAndTenantId(guardian.get().getId(), student.getId(), tenantId)
				.map(StudentGuardianLink::pickupDecision)
				.orElse(PickupDecision.NOT_LINKED);
	}

	@Transactional(readOnly = true)
	public List<PickupAuthorizedGuardian> listAuthorized(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		return linkRepository.findByStudentId(tenantId, student.getId()).stream()
				.filter(link -> link.pickupDecision() == PickupDecision.AUTHORIZED)
				.flatMap(link -> guardianRepository.findByIdAndTenantId(link.getGuardianId(), tenantId)
						.map(guardian -> new PickupAuthorizedGuardian(link, guardian)).stream())
				.toList();
	}

	@Transactional(readOnly = true)
	public List<GuardianAuthorizationChange> history(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		return changeRepository.findAllByStudentIdAndTenantIdOrderByCreatedAtDesc(student.getId(), tenantId);
	}

	private StudentGuardianLink change(String guardianPublicId, String studentPublicId, Long changedByUserId,
			String note, Consumer<StudentGuardianLink> mutation) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Guardian guardian = guardianRepository.findByPublicIdAndTenantId(UUID.fromString(guardianPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Guardian not found: " + guardianPublicId));
		Student student = requireStudent(tenantId, studentPublicId);
		StudentGuardianLink link = linkRepository
				.findByGuardianIdAndStudentIdAndTenantId(guardian.getId(), student.getId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"No guardian-student link found for guardian " + guardianPublicId + " and student "
								+ studentPublicId));

		boolean oldAuthorized = link.isAuthorizedForPickup();
		boolean oldRestricted = link.isCustodyRestricted();
		mutation.accept(link);
		if (oldAuthorized == link.isAuthorizedForPickup() && oldRestricted == link.isCustodyRestricted()) {
			return link;
		}

		StudentGuardianLink saved = linkRepository.save(link);
		GuardianAuthorizationChange record = GuardianAuthorizationChange.record(student.getId(), guardian.getId(),
				oldAuthorized, oldRestricted, saved, note, changedByUserId);
		record.setTenantId(tenantId);
		changeRepository.save(record);
		return saved;
	}

	private Student requireStudent(Long tenantId, String studentPublicId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
