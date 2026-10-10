package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.transport.model.Vehicle;
import com.altafjava.school.domain.transport.repository.VehicleRepository;

@Service
public class VehicleService {

	private final VehicleRepository vehicleRepository;

	public VehicleService(VehicleRepository vehicleRepository) {
		this.vehicleRepository = vehicleRepository;
	}

	@Transactional(readOnly = true)
	public Page<Vehicle> list(Pageable pageable) {
		return vehicleRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Vehicle findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return vehicleRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + publicId));
	}

	@Transactional
	public Vehicle create(String registrationNumber, int capacity, String driverName, String driverContact) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (vehicleRepository.existsByRegistrationNumberAndTenantId(registrationNumber, tenantId)) {
			throw new BusinessException("Vehicle already registered: " + registrationNumber);
		}
		return vehicleRepository.save(Vehicle.create(registrationNumber, capacity, driverName, driverContact));
	}

	@Transactional
	public Vehicle updateDetails(String publicId, int capacity, String driverName, String driverContact,
			ExpectedVersion expectedVersion) {
		Vehicle vehicle = findByPublicId(publicId);
		expectedVersion.verify(vehicle);
		vehicle.updateDetails(capacity, driverName, driverContact);
		return vehicleRepository.save(vehicle);
	}

	@Transactional
	public Vehicle deactivate(String publicId) {
		Vehicle vehicle = findByPublicId(publicId);
		vehicle.deactivate();
		return vehicleRepository.save(vehicle);
	}
}
