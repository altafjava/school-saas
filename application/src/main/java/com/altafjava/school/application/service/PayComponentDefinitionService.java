package com.altafjava.school.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.payroll.model.PayComponentDefinition;
import com.altafjava.school.domain.payroll.model.PayComponentType;
import com.altafjava.school.domain.payroll.repository.PayComponentDefinitionRepository;

@Service
public class PayComponentDefinitionService {

	private final PayComponentDefinitionRepository payComponentDefinitionRepository;

	public PayComponentDefinitionService(PayComponentDefinitionRepository payComponentDefinitionRepository) {
		this.payComponentDefinitionRepository = payComponentDefinitionRepository;
	}

	@Transactional(readOnly = true)
	public List<PayComponentDefinition> list() {
		return payComponentDefinitionRepository
				.findAllByTenantIdOrderByDisplayOrder(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public List<PayComponentDefinition> listActive() {
		return payComponentDefinitionRepository
				.findAllByTenantIdAndActiveTrueOrderByDisplayOrder(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public PayComponentDefinition findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return payComponentDefinitionRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Pay component not found: " + publicId));
	}

	@Transactional
	public PayComponentDefinition create(String code, String name, PayComponentType type, int displayOrder) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (payComponentDefinitionRepository.existsByCodeAndTenantId(code, tenantId)) {
			throw new BusinessException("Pay component already exists: " + code);
		}
		return payComponentDefinitionRepository.save(PayComponentDefinition.create(code, name, type, displayOrder));
	}

	@Transactional
	public PayComponentDefinition update(String publicId, String name, boolean active, int displayOrder,
			ExpectedVersion expectedVersion) {
		PayComponentDefinition definition = findByPublicId(publicId);
		expectedVersion.verify(definition);
		definition.update(name, active, displayOrder);
		return payComponentDefinitionRepository.save(definition);
	}
}
