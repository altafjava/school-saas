package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.altafjava.school.application.filter.AssetFilter;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.inventory.repository.AssetRepository;

@Service
public class AssetService {

	private final AssetRepository assetRepository;

	public AssetService(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	@Transactional(readOnly = true)
	public Page<Asset> list(AssetFilter filter, Pageable pageable) {
		return assetRepository.search(TenantContext.getCurrentTenantId(), filter.status(),
				LikePattern.contains(filter.q()), pageable);
	}

	@Transactional(readOnly = true)
	public Asset findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return assetRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + publicId));
	}

	@Transactional
	public Asset create(String assetCode, String name, String category, LocalDate purchaseDate,
			BigDecimal purchaseCost, String location) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (assetRepository.existsByAssetCodeAndTenantId(assetCode, tenantId)) {
			throw new BusinessException("Asset code already exists: " + assetCode);
		}
		return assetRepository.save(Asset.create(assetCode, name, category, purchaseDate, purchaseCost, location));
	}

	@Transactional
	public Asset updateLocation(String publicId, String location, ExpectedVersion expectedVersion) {
		Asset asset = findByPublicId(publicId);
		expectedVersion.verify(asset);
		asset.updateLocation(location);
		return assetRepository.save(asset);
	}

	@Transactional
	public Asset markUnderMaintenance(String publicId) {
		Asset asset = findByPublicId(publicId);
		asset.markUnderMaintenance();
		return assetRepository.save(asset);
	}

	@Transactional
	public Asset markAvailable(String publicId) {
		Asset asset = findByPublicId(publicId);
		asset.markAvailable();
		return assetRepository.save(asset);
	}

	@Transactional
	public Asset markDisposed(String publicId) {
		Asset asset = findByPublicId(publicId);
		asset.markDisposed();
		return assetRepository.save(asset);
	}
}
