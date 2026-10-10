package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@Service
public class SubjectService {

	private final SubjectRepository subjectRepository;
	private final CurriculumRepository curriculumRepository;

	public SubjectService(SubjectRepository subjectRepository, CurriculumRepository curriculumRepository) {
		this.subjectRepository = subjectRepository;
		this.curriculumRepository = curriculumRepository;
	}

	/** Free-text {@code q} (blank = no filter) over the entity's identifying fields. */
	@Transactional(readOnly = true)
	public Page<Subject> searchSubjects(Pageable pageable, String q) {
		return subjectRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public Page<Subject> listSubjects(Pageable pageable) {
		return subjectRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Subject findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return subjectRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + publicId));
	}

	@Transactional
	public Subject create(String code, String name, String description) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (subjectRepository.existsByCodeAndTenantId(code, tenantId)) {
			throw new BusinessException("Subject code already exists: " + code);
		}
		Subject subject = Subject.create(code, name, description);
		return subjectRepository.save(subject);
	}

	@Transactional
	public Subject deactivate(String publicId) {
		Subject subject = findByPublicId(publicId);
		subject.deactivate();
		return subjectRepository.save(subject);
	}

	@Transactional
	public Subject assignCurriculum(String publicId, String curriculumPublicId, ExpectedVersion expectedVersion) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Subject subject = findByPublicId(publicId);
		expectedVersion.verify(subject);
		var curriculum = curriculumRepository.findByPublicIdAndTenantId(UUID.fromString(curriculumPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Curriculum not found: " + curriculumPublicId));
		subject.assignCurriculum(curriculum.getId());
		return subjectRepository.save(subject);
	}
}
