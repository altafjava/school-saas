package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.hostel.model.HostelBuilding;
import com.altafjava.school.domain.hostel.repository.HostelBuildingRepository;

@Service
public class HostelBuildingService {

	private final HostelBuildingRepository hostelBuildingRepository;

	public HostelBuildingService(HostelBuildingRepository hostelBuildingRepository) {
		this.hostelBuildingRepository = hostelBuildingRepository;
	}

	@Transactional(readOnly = true)
	public Page<HostelBuilding> list(Pageable pageable) {
		return hostelBuildingRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public HostelBuilding findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return hostelBuildingRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Hostel building not found: " + publicId));
	}

	@Transactional
	public HostelBuilding create(String name, String address) {
		return hostelBuildingRepository.save(HostelBuilding.create(name, address));
	}

	@Transactional
	public HostelBuilding updateDetails(String publicId, String name, String address, ExpectedVersion expectedVersion) {
		HostelBuilding building = findByPublicId(publicId);
		expectedVersion.verify(building);
		building.updateDetails(name, address);
		return hostelBuildingRepository.save(building);
	}

	@Transactional
	public HostelBuilding deactivate(String publicId) {
		HostelBuilding building = findByPublicId(publicId);
		building.deactivate();
		return hostelBuildingRepository.save(building);
	}
}
