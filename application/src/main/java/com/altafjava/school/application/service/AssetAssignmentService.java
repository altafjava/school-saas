package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.inventory.model.AssetAssignment;
import com.altafjava.school.domain.inventory.model.AssignedToType;
import com.altafjava.school.domain.inventory.repository.AssetAssignmentRepository;
import com.altafjava.school.domain.inventory.repository.AssetRepository;

@Service
public class AssetAssignmentService {

	private final AssetAssignmentRepository assetAssignmentRepository;
	private final AssetRepository assetRepository;
	private final EmployeeRepository employeeRepository;
	private final ClassroomRepository classroomRepository;

	public AssetAssignmentService(AssetAssignmentRepository assetAssignmentRepository,
			AssetRepository assetRepository, EmployeeRepository employeeRepository,
			ClassroomRepository classroomRepository) {
		this.assetAssignmentRepository = assetAssignmentRepository;
		this.assetRepository = assetRepository;
		this.employeeRepository = employeeRepository;
		this.classroomRepository = classroomRepository;
	}

	@Transactional(readOnly = true)
	public Page<AssetAssignment> listForAsset(String assetPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Asset asset = assetRepository.findByPublicIdAndTenantId(UUID.fromString(assetPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetPublicId));
		return assetAssignmentRepository.findAllByAssetIdAndTenantId(asset.getId(), tenantId, pageable);
	}

	@Transactional
	public AssetAssignment assign(String assetPublicId, AssignedToType assignedToType, String assignedToPublicId,
			LocalDate assignedAt) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Asset asset = assetRepository.findByPublicIdAndTenantId(UUID.fromString(assetPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetPublicId));
		Long assignedToId = requireHolderId(tenantId, assignedToType, assignedToPublicId);
		asset.markInUse();
		assetRepository.save(asset);
		AssetAssignment assignment = AssetAssignment.create(asset.getId(), assignedToType, assignedToId, assignedAt);
		return assetAssignmentRepository.save(assignment);
	}

	private Long requireHolderId(Long tenantId, AssignedToType assignedToType, String holderPublicId) {
		UUID holder = UUID.fromString(holderPublicId);
		if (assignedToType == AssignedToType.STAFF) {
			return employeeRepository.findByPublicIdAndTenantId(holder, tenantId)
					.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + holderPublicId))
					.getId();
		}
		return classroomRepository.findByPublicIdAndTenantId(holder, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + holderPublicId))
				.getId();
	}

	@Transactional
	public AssetAssignment markReturned(String publicId, LocalDate returnedAt) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AssetAssignment assignment = assetAssignmentRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset assignment not found: " + publicId));
		assignment.markReturned(returnedAt);
		AssetAssignment saved = assetAssignmentRepository.save(assignment);

		Asset asset = assetRepository.findByIdAndTenantId(assignment.getAssetId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assignment.getAssetId()));
		asset.markAvailable();
		assetRepository.save(asset);
		return saved;
	}
}
