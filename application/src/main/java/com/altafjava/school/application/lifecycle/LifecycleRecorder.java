package com.altafjava.school.application.lifecycle;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.lifecycle.model.LifecycleStage;
import com.altafjava.school.domain.lifecycle.model.LifecycleTransition;
import com.altafjava.school.domain.lifecycle.repository.LifecycleTransitionRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import lombok.RequiredArgsConstructor;

/**
 * Writes lifecycle history in the caller's transaction, so a status change and its history row
 * commit or roll back together. A component rather than a service: the services that change status
 * all call it, and services must not call each other.
 */
@Component
@RequiredArgsConstructor
public class LifecycleRecorder {

	private final LifecycleTransitionRepository repository;

	public void admission(Long admissionId, AdmissionStatus from, AdmissionStatus to, LifecycleChange change) {
		save(admissionId, null, from == null ? null : LifecycleStage.of(from), LifecycleStage.of(to), change);
	}

	/** The enrollment step: one row that belongs to both the admission and the new student. */
	public void enrolledFromAdmission(Long admissionId, Long studentId, LifecycleChange change) {
		save(admissionId, studentId, LifecycleStage.APPROVED, LifecycleStage.ENROLLED, change);
	}

	/** Saga compensation: the enrollment is undone and the admission is approved-but-not-enrolled again. */
	public void enrollmentRolledBack(Long admissionId, Long studentId, String reason) {
		save(admissionId, studentId, LifecycleStage.ENROLLED, LifecycleStage.APPROVED, LifecycleChange.of(reason));
	}

	public void student(Long studentId, EnrollmentStatus from, EnrollmentStatus to, LifecycleChange change) {
		save(null, studentId, from == null ? null : LifecycleStage.of(from), LifecycleStage.of(to), change);
	}

	public void alumnus(Long studentId, LifecycleChange change) {
		save(null, studentId, LifecycleStage.GRADUATED, LifecycleStage.ALUMNI, change);
	}

	private void save(Long admissionId, Long studentId, LifecycleStage from, LifecycleStage to,
			LifecycleChange change) {
		LifecycleTransition transition = LifecycleTransition.record(admissionId, studentId, from, to, change.reason(),
				change.effectiveOn(), currentUserId());
		transition.setTenantId(TenantContext.getCurrentTenantId());
		repository.save(transition);
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
				? user.getId()
				: null;
	}
}
