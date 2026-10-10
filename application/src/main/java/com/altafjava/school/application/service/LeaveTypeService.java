package com.altafjava.school.application.service;

import java.math.BigDecimal;
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
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;

@Service
public class LeaveTypeService {

	private final LeaveTypeRepository leaveTypeRepository;

	public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
		this.leaveTypeRepository = leaveTypeRepository;
	}

	@Transactional(readOnly = true)
	public Page<LeaveType> list(String q, Pageable pageable) {
		return leaveTypeRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public List<LeaveType> listActive() {
		return leaveTypeRepository.findAllByTenantIdAndActiveTrue(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public LeaveType findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return leaveTypeRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave type not found: " + publicId));
	}

	@Transactional
	public LeaveType create(String name, BigDecimal defaultAnnualDays) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (leaveTypeRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new BusinessException("Leave type already exists: " + name);
		}
		return leaveTypeRepository.save(LeaveType.create(name, defaultAnnualDays));
	}

	@Transactional
	public LeaveType updateDetails(String publicId, String name, BigDecimal defaultAnnualDays,
			ExpectedVersion expectedVersion) {
		LeaveType leaveType = findByPublicId(publicId);
		expectedVersion.verify(leaveType);
		leaveType.updateDetails(name, defaultAnnualDays);
		return leaveTypeRepository.save(leaveType);
	}

	@Transactional
	public LeaveType deactivate(String publicId) {
		LeaveType leaveType = findByPublicId(publicId);
		leaveType.deactivate();
		return leaveTypeRepository.save(leaveType);
	}

	@Transactional
	public LeaveType markUnpaid(String publicId) {
		LeaveType leaveType = findByPublicId(publicId);
		leaveType.markUnpaid();
		return leaveTypeRepository.save(leaveType);
	}

	@Transactional
	public LeaveType markPaid(String publicId) {
		LeaveType leaveType = findByPublicId(publicId);
		leaveType.markPaid();
		return leaveTypeRepository.save(leaveType);
	}

	@Transactional
	public LeaveType restrictDuringProbation(String publicId) {
		LeaveType leaveType = findByPublicId(publicId);
		leaveType.restrictDuringProbation();
		return leaveTypeRepository.save(leaveType);
	}

	@Transactional
	public LeaveType allowDuringProbation(String publicId) {
		LeaveType leaveType = findByPublicId(publicId);
		leaveType.allowDuringProbation();
		return leaveTypeRepository.save(leaveType);
	}

	@Transactional
	public LeaveType configureApprovalLevels(String publicId, int approvalLevels, ExpectedVersion expectedVersion) {
		LeaveType leaveType = findByPublicId(publicId);
		expectedVersion.verify(leaveType);
		leaveType.configureApprovalLevels(approvalLevels);
		return leaveTypeRepository.save(leaveType);
	}

	@Transactional
	public LeaveType configureCarryForward(String publicId, boolean enabled, BigDecimal maxCarryForwardDays,
			Integer carryForwardExpiryMonths, ExpectedVersion expectedVersion) {
		LeaveType leaveType = findByPublicId(publicId);
		expectedVersion.verify(leaveType);
		leaveType.configureCarryForward(enabled, maxCarryForwardDays, carryForwardExpiryMonths);
		return leaveTypeRepository.save(leaveType);
	}
}
