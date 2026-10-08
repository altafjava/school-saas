package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.fee.model.DiscountType;
import com.altafjava.school.domain.fee.model.FeeDiscount;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeDiscountRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Grants and revokes per-student fee concessions. A discount reduces income, so granting is
 * audited and a student's discounts on one fee can never add up to more than the fee itself.
 */
@Service
@RequiredArgsConstructor
public class FeeDiscountService {

	private final FeeDiscountRepository feeDiscountRepository;
	private final StudentRepository studentRepository;
	private final FeeStructureRepository feeStructureRepository;

	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "FeeDiscount", details = "Fee discount granted")
	public FeeDiscount grant(String studentPublicId, String feeStructurePublicId, DiscountType type,
			BigDecimal value, String category, String reason, Long grantedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		FeeStructure structure = feeStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(feeStructurePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Fee structure not found: " + feeStructurePublicId));

		FeeDiscount discount = FeeDiscount.grant(student.getId(), structure.getId(), type, value,
				category.toUpperCase(java.util.Locale.ROOT), reason, grantedByUserId);
		BigDecimal alreadyGranted = feeDiscountRepository
				.findActiveByStudentAndStructure(tenantId, student.getId(), structure.getId()).stream()
				.map(existing -> existing.amountOn(structure.getAmount()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (alreadyGranted.add(discount.amountOn(structure.getAmount())).compareTo(structure.getAmount()) > 0) {
			throw new BusinessException("Discounts on " + structure.getName() + " would exceed the fee of "
					+ structure.getAmount() + " (already granted " + alreadyGranted + ")");
		}
		return feeDiscountRepository.save(discount);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "FeeDiscount", details = "Fee discount revoked")
	public FeeDiscount revoke(String studentPublicId, String discountPublicId, String reason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		FeeDiscount discount = feeDiscountRepository
				.findByPublicIdAndTenantId(UUID.fromString(discountPublicId), tenantId)
				.filter(d -> d.getStudentId().equals(student.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Fee discount not found: " + discountPublicId));
		discount.revoke(reason);
		return feeDiscountRepository.save(discount);
	}

	@Transactional(readOnly = true)
	public List<FeeDiscount> listForStudent(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return feeDiscountRepository.findByStudentId(tenantId, requireStudent(tenantId, studentPublicId).getId());
	}

	private Student requireStudent(Long tenantId, String studentPublicId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
