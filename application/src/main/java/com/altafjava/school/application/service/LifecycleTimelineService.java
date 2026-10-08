package com.altafjava.school.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;
import com.altafjava.school.domain.lifecycle.model.LifecycleTransition;
import com.altafjava.school.domain.lifecycle.repository.LifecycleTransitionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/** Read side of the unified lifecycle history. */
@Service
@RequiredArgsConstructor
public class LifecycleTimelineService {

	private final LifecycleTransitionRepository repository;
	private final StudentRepository studentRepository;
	private final AdmissionRepository admissionRepository;
	private final StudentDataAccessGuard studentDataAccessGuard;

	@Transactional(readOnly = true)
	public List<LifecycleTransition> forStudent(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return repository.findTimelineForStudent(tenantId, student.getId());
	}

	@Transactional(readOnly = true)
	public List<LifecycleTransition> forAdmission(String admissionPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Admission admission = admissionRepository
				.findByPublicIdAndTenantId(UUID.fromString(admissionPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Admission not found: " + admissionPublicId));
		return repository.findAllByAdmissionIdAndTenantIdOrderByCreatedAtAscIdAsc(admission.getId(), tenantId);
	}
}
