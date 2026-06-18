package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;

@Service
public class FeePaymentService {

	private final FeePaymentRepository feePaymentRepository;

	public FeePaymentService(FeePaymentRepository feePaymentRepository) {
		this.feePaymentRepository = feePaymentRepository;
	}

	@Transactional(readOnly = true)
	public Page<FeePayment> listFeePayments(Pageable pageable) {
		return feePaymentRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public FeePayment findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return feePaymentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + publicId));
	}

	@Transactional
	public FeePayment record(Long studentId, Long feeStructureId, BigDecimal paidAmount,
			LocalDateTime paidAt, String receiptNumber) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (feePaymentRepository.existsByReceiptNumberAndTenantId(receiptNumber, tenantId)) {
			throw new IllegalArgumentException("Receipt number already exists: " + receiptNumber);
		}
		FeePayment payment = FeePayment.create(studentId, feeStructureId, paidAmount, paidAt, receiptNumber);
		return feePaymentRepository.save(payment);
	}
}
