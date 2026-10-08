package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayCredentialsDecryptor;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayProviderRegistry;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayResolver;
import com.altafjava.platform.application.paymentgateway.ResolvedPaymentGatewayConfig;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayCredentials;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayProvider;
import com.altafjava.platform.domain.paymentgateway.service.RefundResult;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeRefund;
import com.altafjava.school.domain.fee.model.PaymentSource;
import com.altafjava.school.domain.fee.model.RefundMethod;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeRefundRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Refunds money against a recorded {@link FeePayment}, up to what was paid minus what is already
 * refunded or being refunded. Safe against the two ways refunds go wrong:
 * <ul>
 * <li><b>Double refund</b> — the payment row is locked while the refundable amount is checked and
 * the refund reserved, so concurrent requests serialize.</li>
 * <li><b>Money out, no record</b> — a gateway refund is reserved ({@code PENDING}) and committed
 * <em>before</em> the gateway is called, then settled afterwards. A crash in between leaves a
 * visible {@code PENDING} row that still counts against the payment, never an unrecorded refund.</li>
 * </ul>
 * No transaction is held open across the gateway call.
 */
@Slf4j
@Service
public class FeeRefundService {

	private static final String CREDIT_NOTE_SEQUENCE = "FEE_CREDIT_NOTE";
	// Stripe reports a refund as "pending" while it clears; it has been accepted and will settle.
	private static final List<String> ACCEPTED_GATEWAY_STATUSES = List.of("succeeded", "pending");

	private final FeePaymentRepository feePaymentRepository;
	private final FeeRefundRepository feeRefundRepository;
	private final StudentRepository studentRepository;
	private final NumberSequenceService numberSequenceService;
	private final PaymentGatewayResolver paymentGatewayResolver;
	private final PaymentGatewayProviderRegistry paymentGatewayProviderRegistry;
	private final PaymentGatewayCredentialsDecryptor paymentGatewayCredentialsDecryptor;
	private final TransactionTemplate transactionTemplate;

	public FeeRefundService(FeePaymentRepository feePaymentRepository, FeeRefundRepository feeRefundRepository,
			StudentRepository studentRepository, NumberSequenceService numberSequenceService,
			PaymentGatewayResolver paymentGatewayResolver,
			PaymentGatewayProviderRegistry paymentGatewayProviderRegistry,
			PaymentGatewayCredentialsDecryptor paymentGatewayCredentialsDecryptor,
			PlatformTransactionManager transactionManager) {
		this.feePaymentRepository = feePaymentRepository;
		this.feeRefundRepository = feeRefundRepository;
		this.studentRepository = studentRepository;
		this.numberSequenceService = numberSequenceService;
		this.paymentGatewayResolver = paymentGatewayResolver;
		this.paymentGatewayProviderRegistry = paymentGatewayProviderRegistry;
		this.paymentGatewayCredentialsDecryptor = paymentGatewayCredentialsDecryptor;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Audited(action = AuditAction.CREATE, resourceType = "FeeRefund", details = "Fee refund issued")
	public FeeRefund refund(String paymentPublicId, BigDecimal amount, String reason, Long refundedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		FeePayment payment = feePaymentRepository
				.findByPublicIdAndTenantId(UUID.fromString(paymentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + paymentPublicId));
		RefundMethod method = payment.getPaymentSource() == PaymentSource.GATEWAY ? RefundMethod.GATEWAY
				: RefundMethod.MANUAL;

		FeeRefund reserved = reserve(tenantId, payment.getPublicId(), amount, reason, method, refundedByUserId);
		if (method == RefundMethod.MANUAL) {
			return reserved;
		}
		return settleWithGateway(tenantId, payment, reserved);
	}

	@Transactional(readOnly = true)
	public List<FeeRefund> listForPayment(String paymentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		FeePayment payment = feePaymentRepository
				.findByPublicIdAndTenantId(UUID.fromString(paymentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + paymentPublicId));
		return feeRefundRepository.findAllByFeePaymentIdAndTenantIdOrderByCreatedAtDesc(payment.getId(), tenantId);
	}

	@Transactional(readOnly = true)
	public List<FeeRefund> listForStudent(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return feeRefundRepository.findAllByStudentIdAndTenantIdOrderByCreatedAtDesc(student.getId(), tenantId);
	}

	private FeeRefund reserve(Long tenantId, UUID paymentPublicId, BigDecimal amount, String reason,
			RefundMethod method, Long refundedByUserId) {
		return transactionTemplate.execute(status -> {
			FeePayment payment = feePaymentRepository.findByPublicIdAndTenantIdForUpdate(paymentPublicId, tenantId)
					.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + paymentPublicId));
			BigDecimal alreadyReserved = feeRefundRepository.sumReservedByPayment(tenantId, payment.getId());
			BigDecimal refundable = payment.getPaidAmount().subtract(alreadyReserved);
			if (amount == null || amount.compareTo(refundable) > 0) {
				throw new BusinessException("Refund exceeds the refundable amount of " + refundable
						+ " for this payment (paid " + payment.getPaidAmount() + ", already refunded "
						+ alreadyReserved + ")");
			}
			String creditNote = numberSequenceService.generateNext(tenantId, CREDIT_NOTE_SEQUENCE, "CN-", 6,
					ResetPeriod.YEARLY);
			FeeRefund refund = FeeRefund.reserve(payment, amount, reason, method, creditNote, refundedByUserId);
			refund.setTenantId(tenantId);
			return feeRefundRepository.save(refund);
		});
	}

	private FeeRefund settleWithGateway(Long tenantId, FeePayment payment, FeeRefund reserved) {
		RefundResult result;
		try {
			ResolvedPaymentGatewayConfig config = paymentGatewayResolver.resolve(tenantId)
					.orElseThrow(() -> new BusinessException(
							"No payment gateway configured for this school — cannot refund an online payment"));
			PaymentGatewayCredentials credentials = paymentGatewayCredentialsDecryptor.decrypt(config);
			PaymentGatewayProvider provider = paymentGatewayProviderRegistry.resolve(config.providerType());
			result = provider.refund(credentials, payment.getGatewayChargeReference(), reserved.getAmount());
		} catch (RuntimeException ex) {
			log.error("action=fee-refund-gateway-failed tenantId={} refundId={} paymentId={}", tenantId,
					reserved.getId(), payment.getId(), ex);
			settle(reserved.getId(), tenantId, null, truncate("Gateway call failed: " + ex.getMessage()));
			throw ex instanceof BusinessException ? ex
					: new BusinessException("The payment gateway could not process the refund; nothing was refunded");
		}

		boolean accepted = result.status() != null
				&& ACCEPTED_GATEWAY_STATUSES.contains(result.status().toLowerCase(Locale.ROOT));
		FeeRefund settled = settle(reserved.getId(), tenantId, accepted ? result.gatewayRefundReference() : null,
				accepted ? null : "Gateway rejected the refund: " + result.status());
		if (!accepted) {
			throw new BusinessException("The payment gateway rejected the refund (" + result.status() + ")");
		}
		return settled;
	}

	private FeeRefund settle(Long refundId, Long tenantId, String gatewayReference, String failureReason) {
		return transactionTemplate.execute(status -> {
			FeeRefund refund = feeRefundRepository.findById(refundId).orElseThrow();
			if (failureReason == null) {
				refund.complete(gatewayReference);
			} else {
				refund.fail(failureReason);
			}
			return feeRefundRepository.save(refund);
		});
	}

	private static String truncate(String message) {
		return message != null && message.length() > 500 ? message.substring(0, 500) : message;
	}
}
