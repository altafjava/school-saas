package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayCredentialsDecryptor;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayProviderRegistry;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayResolver;
import com.altafjava.platform.application.paymentgateway.ResolvedPaymentGatewayConfig;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.paymentgateway.model.PaymentGatewayConfirmationMode;
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

@ExtendWith(MockitoExtension.class)
class FeeOnlinePaymentServiceTest {

	private static final String STUDENT_PUBLIC_ID = "11111111-1111-1111-1111-111111111111";
	private static final String FEE_STRUCTURE_PUBLIC_ID = "22222222-2222-2222-2222-222222222222";
	private static final String GATEWAY_CHARGE_REFERENCE = "pi_test_123";

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;
	@Mock
	private FeePaymentRepository feePaymentRepository;
	@Mock
	private TenantRepository tenantRepository;
	@Mock
	private FeePaymentService feePaymentService;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private PaymentGatewayResolver paymentGatewayResolver;
	@Mock
	private PaymentGatewayProviderRegistry paymentGatewayProviderRegistry;
	@Mock
	private PaymentGatewayCredentialsDecryptor paymentGatewayCredentialsDecryptor;
	@Mock
	private PaymentGatewayProvider paymentGatewayProvider;

	private FeeOnlinePaymentService service;

	@BeforeEach
	void setUp() {
		service = new FeeOnlinePaymentService(studentRepository, feeStructureRepository, feePaymentRepository,
				tenantRepository, feePaymentService, studentDataAccessGuard, paymentGatewayResolver,
				paymentGatewayProviderRegistry, paymentGatewayCredentialsDecryptor);
		TenantContext.ForTesting.setCurrentTenant(1L, UUID.randomUUID(), "school-a", TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student student() {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(10L);
		return student;
	}

	private FeeStructure feeStructure() {
		FeeStructure feeStructure = FeeStructure.create("Tuition", BigDecimal.valueOf(1000),
				com.altafjava.school.domain.fee.model.FeeFrequency.MONTHLY, "Standard");
		feeStructure.setId(20L);
		return feeStructure;
	}

	private ResolvedPaymentGatewayConfig resolvedConfig() {
		return new ResolvedPaymentGatewayConfig(PaymentGatewayType.STRIPE, "sandbox",
				PaymentGatewayConfirmationMode.BOTH, "ciphertext");
	}

	private PaymentGatewayCredentials credentials() {
		return new PaymentGatewayCredentials(PaymentGatewayType.STRIPE, "sandbox", Map.of());
	}

	@Test
	void createCharge_withNoGatewayConfigured_throwsBusinessException() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student()));
		when(feeStructureRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(feeStructure()));
		when(feePaymentService.calculateBalanceForStudent(eq(1L), any())).thenReturn(List.of(
				new FeeBalance(20L, "Tuition", BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(1000),
						BigDecimal.ZERO, BigDecimal.ZERO, null)));
		when(paymentGatewayResolver.resolve(1L)).thenReturn(Optional.empty());

		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.createCharge(STUDENT_PUBLIC_ID, FEE_STRUCTURE_PUBLIC_ID));

		assertTrue(exception.getMessage().contains("No payment gateway configured"));
		verify(paymentGatewayProviderRegistry, never()).resolve(any());
	}

	@Test
	void createCharge_withNoOutstandingBalance_throwsBusinessException() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student()));
		when(feeStructureRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(feeStructure()));
		when(feePaymentService.calculateBalanceForStudent(eq(1L), any())).thenReturn(List.of(
				new FeeBalance(20L, "Tuition", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, null)));

		assertThrows(BusinessException.class, () -> service.createCharge(STUDENT_PUBLIC_ID, FEE_STRUCTURE_PUBLIC_ID));
		verify(paymentGatewayResolver, never()).resolve(any());
	}

	@Test
	void createCharge_withOutstandingBalanceAndGatewayConfigured_delegatesToProvider() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student()));
		when(feeStructureRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(feeStructure()));
		when(feePaymentService.calculateBalanceForStudent(eq(1L), any())).thenReturn(List.of(
				new FeeBalance(20L, "Tuition", BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(1000),
						BigDecimal.ZERO, BigDecimal.ZERO, null)));
		when(paymentGatewayResolver.resolve(1L)).thenReturn(Optional.of(resolvedConfig()));
		when(paymentGatewayCredentialsDecryptor.decrypt(any())).thenReturn(credentials());
		when(paymentGatewayProviderRegistry.resolve(PaymentGatewayType.STRIPE)).thenReturn(paymentGatewayProvider);
		when(paymentGatewayProvider.createCharge(any(), any()))
				.thenReturn(new PaymentChargeResult(GATEWAY_CHARGE_REFERENCE, "requires_confirmation", "secret_abc"));
		Tenant tenant = mock(Tenant.class);
		when(tenant.getCurrencyCode()).thenReturn("INR");
		when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

		PaymentChargeResult result = service.createCharge(STUDENT_PUBLIC_ID, FEE_STRUCTURE_PUBLIC_ID);

		assertEquals(GATEWAY_CHARGE_REFERENCE, result.gatewayChargeReference());
		verify(studentDataAccessGuard).assertCanView(1L, STUDENT_PUBLIC_ID);
		verify(paymentGatewayProvider).createCharge(eq(credentials()), any(PaymentChargeRequest.class));
	}

	@Test
	void confirmCharge_alreadyRecorded_returnsExistingPaymentIdempotently() {
		FeePayment existing = FeePayment.recordFromGateway(10L, 20L, BigDecimal.valueOf(1000), LocalDateTime.now(),
				"GTW-" + GATEWAY_CHARGE_REFERENCE, "STRIPE", GATEWAY_CHARGE_REFERENCE);
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student()));
		when(feeStructureRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(feeStructure()));
		when(feePaymentRepository.findByGatewayChargeReferenceAndTenantId(GATEWAY_CHARGE_REFERENCE, 1L))
				.thenReturn(Optional.of(existing));

		FeePayment result = service.confirmCharge(STUDENT_PUBLIC_ID, FEE_STRUCTURE_PUBLIC_ID, GATEWAY_CHARGE_REFERENCE);

		assertEquals(existing, result);
		verify(feePaymentRepository, never()).save(any());
		verify(paymentGatewayResolver, never()).resolve(any());
	}

	@Test
	void confirmCharge_withSucceededStatus_recordsFeePayment() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student()));
		when(feeStructureRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(feeStructure()));
		when(feePaymentRepository.findByGatewayChargeReferenceAndTenantId(GATEWAY_CHARGE_REFERENCE, 1L))
				.thenReturn(Optional.empty());
		when(paymentGatewayResolver.resolve(1L)).thenReturn(Optional.of(resolvedConfig()));
		when(paymentGatewayCredentialsDecryptor.decrypt(any())).thenReturn(credentials());
		when(paymentGatewayProviderRegistry.resolve(PaymentGatewayType.STRIPE)).thenReturn(paymentGatewayProvider);
		when(paymentGatewayProvider.getChargeStatus(any(), eq(GATEWAY_CHARGE_REFERENCE)))
				.thenReturn(new PaymentChargeStatus(GATEWAY_CHARGE_REFERENCE, "succeeded", BigDecimal.valueOf(1000),
						"USD"));
		when(feePaymentRepository.save(any(FeePayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FeePayment result = service.confirmCharge(STUDENT_PUBLIC_ID, FEE_STRUCTURE_PUBLIC_ID, GATEWAY_CHARGE_REFERENCE);

		assertEquals(com.altafjava.school.domain.fee.model.PaymentSource.GATEWAY, result.getPaymentSource());
		assertEquals(GATEWAY_CHARGE_REFERENCE, result.getGatewayChargeReference());
	}

	@Test
	void confirmCharge_withPendingStatus_throwsBusinessException() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student()));
		when(feeStructureRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(feeStructure()));
		when(feePaymentRepository.findByGatewayChargeReferenceAndTenantId(GATEWAY_CHARGE_REFERENCE, 1L))
				.thenReturn(Optional.empty());
		when(paymentGatewayResolver.resolve(1L)).thenReturn(Optional.of(resolvedConfig()));
		when(paymentGatewayCredentialsDecryptor.decrypt(any())).thenReturn(credentials());
		when(paymentGatewayProviderRegistry.resolve(PaymentGatewayType.STRIPE)).thenReturn(paymentGatewayProvider);
		when(paymentGatewayProvider.getChargeStatus(any(), eq(GATEWAY_CHARGE_REFERENCE)))
				.thenReturn(new PaymentChargeStatus(GATEWAY_CHARGE_REFERENCE, "requires_payment_method",
						BigDecimal.valueOf(1000), "USD"));

		assertThrows(BusinessException.class,
				() -> service.confirmCharge(STUDENT_PUBLIC_ID, FEE_STRUCTURE_PUBLIC_ID, GATEWAY_CHARGE_REFERENCE));
		verify(feePaymentRepository, never()).save(any());
	}

	@Test
	void recordFromWebhookEvent_withNonSuccessStatus_ignoredGracefully() {
		PaymentWebhookEvent event = new PaymentWebhookEvent("payment_intent.created", GATEWAY_CHARGE_REFERENCE,
				"requires_payment_method", Map.of());

		Optional<FeePayment> result = service.recordFromWebhookEvent(event);

		assertTrue(result.isEmpty());
		verify(feePaymentRepository, never()).save(any());
	}

	@Test
	void recordFromWebhookEvent_alreadyRecorded_isIdempotent() {
		FeePayment existing = FeePayment.recordFromGateway(10L, 20L, BigDecimal.valueOf(1000), LocalDateTime.now(),
				"GTW-" + GATEWAY_CHARGE_REFERENCE, "STRIPE", GATEWAY_CHARGE_REFERENCE);
		PaymentWebhookEvent event = new PaymentWebhookEvent("payment_intent.succeeded", GATEWAY_CHARGE_REFERENCE,
				"succeeded", Map.of("studentPublicId", STUDENT_PUBLIC_ID, "feeStructurePublicId",
						FEE_STRUCTURE_PUBLIC_ID));
		when(feePaymentRepository.findByGatewayChargeReferenceAndTenantId(GATEWAY_CHARGE_REFERENCE, 1L))
				.thenReturn(Optional.of(existing));

		Optional<FeePayment> result = service.recordFromWebhookEvent(event);

		assertEquals(existing, result.orElseThrow());
		verify(feePaymentRepository, never()).save(any());
		verify(paymentGatewayResolver, never()).resolve(any());
	}

	@Test
	void recordFromWebhookEvent_withSucceededStatusAndMetadata_recordsFeePayment() {
		PaymentWebhookEvent event = new PaymentWebhookEvent("payment_intent.succeeded", GATEWAY_CHARGE_REFERENCE,
				"succeeded", Map.of("studentPublicId", STUDENT_PUBLIC_ID, "feeStructurePublicId",
						FEE_STRUCTURE_PUBLIC_ID));
		when(feePaymentRepository.findByGatewayChargeReferenceAndTenantId(GATEWAY_CHARGE_REFERENCE, 1L))
				.thenReturn(Optional.empty());
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student()));
		when(feeStructureRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(feeStructure()));
		when(paymentGatewayResolver.resolve(1L)).thenReturn(Optional.of(resolvedConfig()));
		when(paymentGatewayCredentialsDecryptor.decrypt(any())).thenReturn(credentials());
		when(paymentGatewayProviderRegistry.resolve(PaymentGatewayType.STRIPE)).thenReturn(paymentGatewayProvider);
		when(paymentGatewayProvider.getChargeStatus(any(), eq(GATEWAY_CHARGE_REFERENCE)))
				.thenReturn(new PaymentChargeStatus(GATEWAY_CHARGE_REFERENCE, "succeeded", BigDecimal.valueOf(1000),
						"USD"));
		when(feePaymentRepository.save(any(FeePayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Optional<FeePayment> result = service.recordFromWebhookEvent(event);

		assertTrue(result.isPresent());
		assertEquals(GATEWAY_CHARGE_REFERENCE, result.get().getGatewayChargeReference());
	}

	@Test
	void findReceiptForSelfService_delegatesToGuard() {
		FeePayment payment = FeePayment.recordFromGateway(10L, 20L, BigDecimal.valueOf(1000), LocalDateTime.now(),
				"GTW-" + GATEWAY_CHARGE_REFERENCE, "STRIPE", GATEWAY_CHARGE_REFERENCE);
		UUID publicId = UUID.randomUUID();
		payment.setPublicId(publicId);
		Student student = student();
		student.setPublicId(UUID.fromString(STUDENT_PUBLIC_ID));
		when(feePaymentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(payment));
		when(studentRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(student));

		FeePayment result = assertDoesNotThrow(() -> service.findReceiptForSelfService(publicId.toString()));

		assertEquals(payment, result);
		verify(studentDataAccessGuard).assertCanView(1L, STUDENT_PUBLIC_ID);
	}

	@Test
	void findReceiptForSelfService_notFound_throwsResourceNotFoundException() {
		UUID publicId = UUID.randomUUID();
		when(feePaymentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.findReceiptForSelfService(publicId.toString()));
	}
}
