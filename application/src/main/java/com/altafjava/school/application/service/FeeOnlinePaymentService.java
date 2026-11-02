package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayCredentialsDecryptor;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayProviderRegistry;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayResolver;
import com.altafjava.platform.application.paymentgateway.ResolvedPaymentGatewayConfig;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.paymentgateway.model.PaymentGatewayType;
import com.altafjava.platform.domain.paymentgateway.service.PaymentChargeRequest;
import com.altafjava.platform.domain.paymentgateway.service.PaymentChargeResult;
import com.altafjava.platform.domain.paymentgateway.service.PaymentChargeStatus;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayCredentials;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayProvider;
import com.altafjava.platform.domain.paymentgateway.service.PaymentWebhookEvent;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns a successful payment-gateway charge into a {@link FeePayment} — school-saas owns the
 * business meaning of "this student's fee is paid," while platform-saas's payment-gateway
 * capability (see {@link PaymentGatewayResolver}/{@link PaymentGatewayProviderRegistry}) owns how
 * a charge is created/confirmed with the tenant's configured provider.
 * <p>
 * Outstanding-balance computation is deliberately reused from {@link FeePaymentService}, not
 * duplicated — {@code calculateBalanceForStudent} is the internal, guard-free entrypoint that
 * method already exposes for exactly this kind of trusted, already-resolved-student caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeOnlinePaymentService {

	private static final String SUCCEEDED_STATUS = "succeeded";
	private static final String METADATA_TENANT_PUBLIC_ID = "tenantPublicId";
	private static final String METADATA_STUDENT_PUBLIC_ID = "studentPublicId";
	private static final String METADATA_FEE_STRUCTURE_PUBLIC_ID = "feeStructurePublicId";

	private final StudentRepository studentRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final FeePaymentRepository feePaymentRepository;
	private final TenantRepository tenantRepository;
	private final FeePaymentService feePaymentService;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final PaymentGatewayResolver paymentGatewayResolver;
	private final PaymentGatewayProviderRegistry paymentGatewayProviderRegistry;
	private final PaymentGatewayCredentialsDecryptor paymentGatewayCredentialsDecryptor;

	/**
	 * Creates a gateway charge for a student's outstanding balance on one fee structure. Read-only
	 * — no {@link FeePayment} exists until the charge is confirmed, either synchronously via
	 * {@link #confirmCharge} or asynchronously via {@link #recordFromWebhookEvent}.
	 */
	@Transactional(readOnly = true)
	public PaymentChargeResult createCharge(String studentPublicId, String feeStructurePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(tenantId, studentPublicId);
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		FeeStructure feeStructure = resolveFeeStructure(tenantId, feeStructurePublicId);

		BigDecimal outstandingBalance = resolveOutstandingBalance(tenantId, student, feeStructure.getId());
		if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("No outstanding balance for this fee structure");
		}

		ResolvedPaymentGatewayConfig resolvedConfig = resolveGatewayConfig(tenantId);
		PaymentGatewayCredentials credentials = paymentGatewayCredentialsDecryptor.decrypt(resolvedConfig);
		PaymentGatewayProvider provider = paymentGatewayProviderRegistry.resolve(resolvedConfig.providerType());

		Map<String, String> metadata = Map.of(
				METADATA_TENANT_PUBLIC_ID, TenantContext.getCurrentTenantPublicId().toString(),
				METADATA_STUDENT_PUBLIC_ID, studentPublicId,
				METADATA_FEE_STRUCTURE_PUBLIC_ID, feeStructurePublicId);
		PaymentChargeRequest request = new PaymentChargeRequest(outstandingBalance, resolveCurrency(tenantId),
				metadata, "Fee payment: " + feeStructure.getName());

		return provider.createCharge(credentials, request);
	}

	/**
	 * Synchronous confirmation path (confirmationMode SYNCHRONOUS/BOTH): re-verifies the charge's
	 * status directly with the gateway rather than trusting the client's say-so, then idempotently
	 * records the {@link FeePayment}. {@code studentPublicId}/{@code feeStructurePublicId} are
	 * supplied by the caller (the same values it used to create the charge) because
	 * {@link PaymentGatewayProvider#getChargeStatus} intentionally carries no metadata — only
	 * {@link PaymentWebhookEvent} does, which {@link #recordFromWebhookEvent} relies on instead.
	 */
	@Transactional
	public FeePayment confirmCharge(String studentPublicId, String feeStructurePublicId,
			String gatewayChargeReference) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(tenantId, studentPublicId);
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		FeeStructure feeStructure = resolveFeeStructure(tenantId, feeStructurePublicId);

		Optional<FeePayment> existing = feePaymentRepository.findByGatewayChargeReferenceAndTenantId(
				gatewayChargeReference, tenantId);
		if (existing.isPresent()) {
			return existing.get();
		}

		ResolvedPaymentGatewayConfig resolvedConfig = resolveGatewayConfig(tenantId);
		PaymentGatewayCredentials credentials = paymentGatewayCredentialsDecryptor.decrypt(resolvedConfig);
		PaymentGatewayProvider provider = paymentGatewayProviderRegistry.resolve(resolvedConfig.providerType());

		PaymentChargeStatus status = provider.getChargeStatus(credentials, gatewayChargeReference);
		if (!isSucceeded(status.status())) {
			throw new BusinessException("Payment not completed yet: status=" + status.status());
		}

		return recordGatewayPayment(student.getId(), feeStructure.getId(), status.amount(),
				resolvedConfig.providerType(), gatewayChargeReference);
	}

	/**
	 * Webhook-driven confirmation path (confirmationMode WEBHOOK/BOTH). Called by
	 * {@code FeePaymentWebhookController} only after signature verification has already succeeded
	 * and tenant context is active — {@code event} itself is still not trusted for the paid
	 * amount, which is re-fetched from the gateway via {@link PaymentGatewayProvider#getChargeStatus}
	 * the same way {@link #confirmCharge} does. Non-success events are ignored, not errored, so the
	 * webhook controller can always return 200 quickly.
	 */
	@Transactional
	public Optional<FeePayment> recordFromWebhookEvent(PaymentWebhookEvent event) {
		if (!isSucceeded(event.status())) {
			log.info("action=fee_payment_webhook_ignored status={}", event.status());
			return Optional.empty();
		}
		Long tenantId = TenantContext.getCurrentTenantId();
		Optional<FeePayment> existing = feePaymentRepository.findByGatewayChargeReferenceAndTenantId(
				event.gatewayChargeReference(), tenantId);
		if (existing.isPresent()) {
			return existing;
		}

		String studentPublicId = event.metadata() == null ? null : event.metadata().get(METADATA_STUDENT_PUBLIC_ID);
		String feeStructurePublicId = event.metadata() == null ? null
				: event.metadata().get(METADATA_FEE_STRUCTURE_PUBLIC_ID);
		if (studentPublicId == null || feeStructurePublicId == null) {
			log.warn("action=fee_payment_webhook_missing_metadata reference={}", event.gatewayChargeReference());
			return Optional.empty();
		}

		Student student = resolveStudent(tenantId, studentPublicId);
		FeeStructure feeStructure = resolveFeeStructure(tenantId, feeStructurePublicId);

		ResolvedPaymentGatewayConfig resolvedConfig = resolveGatewayConfig(tenantId);
		PaymentGatewayCredentials credentials = paymentGatewayCredentialsDecryptor.decrypt(resolvedConfig);
		PaymentGatewayProvider provider = paymentGatewayProviderRegistry.resolve(resolvedConfig.providerType());
		PaymentChargeStatus status = provider.getChargeStatus(credentials, event.gatewayChargeReference());

		return Optional.of(recordGatewayPayment(student.getId(), feeStructure.getId(), status.amount(),
				resolvedConfig.providerType(), event.gatewayChargeReference()));
	}

	/** Guard-checked receipt read for the student themselves or their linked guardian. */
	@Transactional(readOnly = true)
	public Page<FeePayment> listForSelfService(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(tenantId, studentPublicId);
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return feePaymentRepository.search(tenantId, student.getId(), null, null, null, null, null, pageable);
	}

	@Transactional(readOnly = true)
	public FeePayment findReceiptForSelfService(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		FeePayment payment = feePaymentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + publicId));
		Student student = studentRepository.findByIdAndTenantId(payment.getStudentId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + publicId));
		studentDataAccessGuard.assertCanView(tenantId, student.getPublicId().toString());
		return payment;
	}

	private FeePayment recordGatewayPayment(Long studentId, Long feeStructureId, BigDecimal amount,
			PaymentGatewayType providerType, String gatewayChargeReference) {
		String receiptNumber = "GTW-" + gatewayChargeReference;
		FeePayment payment = FeePayment.recordFromGateway(studentId, feeStructureId, amount, LocalDateTime.now(),
				receiptNumber, providerType.name(), gatewayChargeReference);
		return feePaymentRepository.save(payment);
	}

	private BigDecimal resolveOutstandingBalance(Long tenantId, Student student, Long feeStructureId) {
		return feePaymentService.calculateBalanceForStudent(tenantId, student).stream()
				.filter(balance -> feeStructureId.equals(balance.feeStructureId()))
				.map(FeeBalance::outstandingBalance)
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(
						"No fee assignment found for this student and fee structure"));
	}

	private ResolvedPaymentGatewayConfig resolveGatewayConfig(Long tenantId) {
		return paymentGatewayResolver.resolve(tenantId)
				.orElseThrow(() -> new BusinessException(
						"No payment gateway configured for this school — contact the school office"));
	}

	private String resolveCurrency(Long tenantId) {
		return tenantRepository.findById(tenantId)
				.map(Tenant::getCurrencyCode)
				.filter(code -> code != null && !code.isBlank())
				.orElseThrow(() -> new BusinessException("This school has no currency configured"));
	}

	private boolean isSucceeded(String status) {
		return SUCCEEDED_STATUS.equalsIgnoreCase(status);
	}

	private Student resolveStudent(Long tenantId, String studentPublicId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}

	private FeeStructure resolveFeeStructure(Long tenantId, String feeStructurePublicId) {
		return feeStructureRepository.findByPublicIdAndTenantId(UUID.fromString(feeStructurePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeeStructure not found: " + feeStructurePublicId));
	}
}
