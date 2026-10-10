package com.altafjava.school.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;

@Service
public class ExamTypeDefinitionService {

	private final ExamTypeDefinitionRepository examTypeDefinitionRepository;

	public ExamTypeDefinitionService(ExamTypeDefinitionRepository examTypeDefinitionRepository) {
		this.examTypeDefinitionRepository = examTypeDefinitionRepository;
	}

	@Transactional(readOnly = true)
	public List<ExamTypeDefinition> list() {
		return examTypeDefinitionRepository.findAllByTenantIdOrderByDisplayOrder(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public List<ExamTypeDefinition> listActive() {
		return examTypeDefinitionRepository
				.findAllByTenantIdAndActiveTrueOrderByDisplayOrder(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public ExamTypeDefinition findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return examTypeDefinitionRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam type not found: " + publicId));
	}

	@Transactional
	public ExamTypeDefinition create(String code, String name, int displayOrder) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (examTypeDefinitionRepository.existsByCodeAndTenantId(code, tenantId)) {
			throw new BusinessException("Exam type already exists: " + code);
		}
		return examTypeDefinitionRepository.save(ExamTypeDefinition.create(code, name, displayOrder));
	}

	@Transactional
	public ExamTypeDefinition update(String publicId, String name, boolean active, int displayOrder,
			ExpectedVersion expectedVersion) {
		ExamTypeDefinition definition = findByPublicId(publicId);
		expectedVersion.verify(definition);
		definition.update(name, active, displayOrder);
		return examTypeDefinitionRepository.save(definition);
	}
}
