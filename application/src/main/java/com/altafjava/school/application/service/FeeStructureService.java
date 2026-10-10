package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.model.FeeStructureRevision;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRevisionRepository;

@Service
public class FeeStructureService {

	private final FeeStructureRepository feeStructureRepository;
	private final FeeStructureRevisionRepository feeStructureRevisionRepository;

	public FeeStructureService(FeeStructureRepository feeStructureRepository,
			FeeStructureRevisionRepository feeStructureRevisionRepository) {
		this.feeStructureRepository = feeStructureRepository;
		this.feeStructureRevisionRepository = feeStructureRevisionRepository;
	}

	@Transactional(readOnly = true)
	public Page<FeeStructure> listFeeStructures(String q, Pageable pageable) {
		return feeStructureRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
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

	// Records a FeeStructureRevision with the pre-revision amount before mutating, so a fee-amount
	// dispute is answerable from history data rather than only visible as an opaque updatedAt bump.
	@Transactional
	public FeeStructure reviseAmount(String publicId, BigDecimal amount, ExpectedVersion expectedVersion) {
		FeeStructure feeStructure = findByPublicId(publicId);
		expectedVersion.verify(feeStructure);
		feeStructureRevisionRepository
				.save(FeeStructureRevision.record(feeStructure.getId(), feeStructure.getAmount(), amount));
		feeStructure.reviseAmount(amount);
		return feeStructureRepository.save(feeStructure);
	}

	@Transactional(readOnly = true)
	public Page<FeeStructureRevision> listRevisions(String publicId, Pageable pageable) {
		FeeStructure feeStructure = findByPublicId(publicId);
		return feeStructureRevisionRepository.findByFeeStructureIdAndTenantId(TenantContext.getCurrentTenantId(),
				feeStructure.getId(), pageable);
	}

	@Transactional
	public FeeStructure configureLateFeePolicy(String publicId, Integer graceDays, BigDecimal lateFeePercentage,
			ExpectedVersion expectedVersion) {
		FeeStructure feeStructure = findByPublicId(publicId);
		expectedVersion.verify(feeStructure);
		feeStructure.configureLateFeePolicy(graceDays, lateFeePercentage);
		return feeStructureRepository.save(feeStructure);
	}
}
