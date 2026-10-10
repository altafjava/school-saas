package com.altafjava.school.application.service;

import java.util.List;
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
import com.altafjava.school.domain.certificate.model.CertificateType;
import com.altafjava.school.domain.certificate.repository.CertificateTypeRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CertificateTypeService {

	private final CertificateTypeRepository certificateTypeRepository;

	@Transactional(readOnly = true)
	public Page<CertificateType> list(String q, Pageable pageable) {
		return certificateTypeRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public List<CertificateType> listActive() {
		return certificateTypeRepository.findAllByTenantIdAndActiveTrue(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public CertificateType findByPublicId(String publicId) {
		return certificateTypeRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), TenantContext.getCurrentTenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Certificate type not found: " + publicId));
	}

	@Transactional
	public CertificateType create(String code, String name, String wording) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (certificateTypeRepository.existsByCodeAndTenantId(code, tenantId)) {
			throw new BusinessException("Certificate type code already exists: " + code);
		}
		if (certificateTypeRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new BusinessException("Certificate type already exists: " + name);
		}
		return certificateTypeRepository.save(CertificateType.create(code, name, wording));
	}

	@Transactional
	public CertificateType updateDetails(String publicId, String name, String wording,
			ExpectedVersion expectedVersion) {
		CertificateType type = findByPublicId(publicId);
		expectedVersion.verify(type);
		type.updateDetails(name, wording);
		return certificateTypeRepository.save(type);
	}

	@Transactional
	public CertificateType activate(String publicId) {
		CertificateType type = findByPublicId(publicId);
		type.activate();
		return certificateTypeRepository.save(type);
	}

	@Transactional
	public CertificateType deactivate(String publicId) {
		CertificateType type = findByPublicId(publicId);
		type.deactivate();
		return certificateTypeRepository.save(type);
	}
}
