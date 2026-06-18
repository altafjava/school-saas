package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;

@Service
public class FeeStructureService {

	private final FeeStructureRepository feeStructureRepository;

	public FeeStructureService(FeeStructureRepository feeStructureRepository) {
		this.feeStructureRepository = feeStructureRepository;
	}

	@Transactional(readOnly = true)
	public Page<FeeStructure> listFeeStructures(Pageable pageable) {
		return feeStructureRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public FeeStructure findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return feeStructureRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeeStructure not found: " + publicId));
	}

	@Transactional
	public FeeStructure create(String name, BigDecimal amount, FeeFrequency frequency, String planType) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (feeStructureRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new IllegalArgumentException("Fee structure already exists: " + name);
		}
		FeeStructure feeStructure = FeeStructure.create(name, amount, frequency, planType);
		return feeStructureRepository.save(feeStructure);
	}
}
