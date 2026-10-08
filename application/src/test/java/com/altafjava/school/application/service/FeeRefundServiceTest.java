package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayCredentialsDecryptor;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayProviderRegistry;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayResolver;
import com.altafjava.platform.application.paymentgateway.ResolvedPaymentGatewayConfig;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayCredentials;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayProvider;
import com.altafjava.platform.domain.paymentgateway.service.RefundResult;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeRefund;
import com.altafjava.school.domain.fee.model.RefundMethod;
import com.altafjava.school.domain.fee.model.RefundStatus;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeRefundRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeRefundServiceTest {

	private static final UUID PAYMENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private FeePaymentRepository feePaymentRepository;
	@Mock
	private FeeRefundRepository feeRefundRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private NumberSequenceService numberSequenceService;
	@Mock
	private PaymentGatewayResolver paymentGatewayResolver;
	@Mock
	private PaymentGatewayProviderRegistry paymentGatewayProviderRegistry;
	@Mock
	private PaymentGatewayCredentialsDecryptor paymentGatewayCredentialsDecryptor;
	@Mock
	private PlatformTransactionManager transactionManager;
	@Mock
	private PaymentGatewayProvider provider;

	private FeeRefundService service;
	private FeeRefund lastSaved;

	@BeforeEach
	void setUp() {
		lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
		service = new FeeRefundService(feePaymentRepository, feeRefundRepository, studentRepository,
				numberSequenceService, paymentGatewayResolver, paymentGatewayProviderRegistry,
				paymentGatewayCredentialsDecryptor, transactionManager);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		lenient().when(numberSequenceService.generateNext(anyLong(), anyString(), anyString(), anyInt(), any()))
				.thenReturn("CN-000001");
		// The service reserves a row, then reloads it by id to settle it — model the table with one slot.
		lenient().when(feeRefundRepository.save(any(FeeRefund.class))).thenAnswer(inv -> {
			lastSaved = inv.getArgument(0);
			return lastSaved;
		});
		lenient().when(feeRefundRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(lastSaved));
	}

	@AfterEach
	void clear() {
		TenantContext.ForTesting.clear();
	}

	private FeePayment payment(boolean gateway) {
		FeePayment payment = gateway
				? FeePayment.recordFromGateway(5L, 6L, new BigDecimal("500.00"), LocalDateTime.now(), "RCPT-1",
						"STRIPE", "ch_1")
				: FeePayment.create(5L, 6L, new BigDecimal("500.00"), LocalDateTime.now(), "RCPT-1");
		payment.setId(7L);
		payment.setPublicId(PAYMENT_PUBLIC_ID);
		lenient().when(feePaymentRepository.findByPublicIdAndTenantId(PAYMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(payment));
		lenient().when(feePaymentRepository.findByPublicIdAndTenantIdForUpdate(PAYMENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(payment));
		return payment;
	}

	private void alreadyRefunded(String amount) {
		when(feeRefundRepository.sumReservedByPayment(1L, 7L)).thenReturn(new BigDecimal(amount));
	}

	private void gatewayReady(RefundResult result) {
		ResolvedPaymentGatewayConfig config = mock(ResolvedPaymentGatewayConfig.class);
		when(paymentGatewayResolver.resolve(1L)).thenReturn(Optional.of(config));
		when(paymentGatewayCredentialsDecryptor.decrypt(config)).thenReturn(mock(PaymentGatewayCredentials.class));
		when(paymentGatewayProviderRegistry.resolve(any())).thenReturn(provider);
		when(provider.refund(any(), eq("ch_1"), any())).thenReturn(result);
	}

	@Test
	void manualPayment_refundIsRecordedAndCompletedWithoutTouchingTheGateway() {
		payment(false);
		alreadyRefunded("0");

		FeeRefund refund = service.refund(PAYMENT_PUBLIC_ID.toString(), new BigDecimal("120.00"), "overcharged", 9L);

		assertEquals(RefundStatus.COMPLETED, refund.getStatus());
		assertEquals(RefundMethod.MANUAL, refund.getMethod());
		assertEquals("CN-000001", refund.getCreditNoteNumber());
		assertEquals(1L, refund.getTenantId());
		verify(paymentGatewayResolver, never()).resolve(any());
	}

	@Test
	void refund_cannotExceedWhatIsStillRefundable() {
		payment(false);
		alreadyRefunded("450.00");

		BusinessException ex = assertThrows(BusinessException.class,
				() -> service.refund(PAYMENT_PUBLIC_ID.toString(), new BigDecimal("60.00"), "x", 9L));

		assertTrue(ex.getMessage().contains("50.00"), ex.getMessage());
		verify(feeRefundRepository, never()).save(any());
	}

	@Test
	void refund_locksThePaymentRowWhileCheckingTheRefundableAmount() {
		payment(false);
		alreadyRefunded("0");

		service.refund(PAYMENT_PUBLIC_ID.toString(), new BigDecimal("10.00"), "x", 9L);

		verify(feePaymentRepository).findByPublicIdAndTenantIdForUpdate(PAYMENT_PUBLIC_ID, 1L);
	}

	@Test
	void gatewayRefund_isReservedAndCommittedBeforeTheGatewayIsCalled() {
		payment(true);
		alreadyRefunded("0");
		gatewayReady(new RefundResult("re_1", "succeeded"));

		FeeRefund refund = service.refund(PAYMENT_PUBLIC_ID.toString(), new BigDecimal("200.00"), "cancelled", 9L);

		InOrder order = inOrder(feeRefundRepository, provider);
		order.verify(feeRefundRepository).save(any(FeeRefund.class));
		order.verify(provider).refund(any(), eq("ch_1"), eq(new BigDecimal("200.00")));
		order.verify(feeRefundRepository).save(any(FeeRefund.class));
		assertEquals(RefundStatus.COMPLETED, refund.getStatus());
		assertEquals("re_1", refund.getGatewayRefundReference());
	}

	@Test
	void gatewayRefund_pendingAtTheGatewayCountsAsAccepted() {
		payment(true);
		alreadyRefunded("0");
		gatewayReady(new RefundResult("re_2", "pending"));

		FeeRefund refund = service.refund(PAYMENT_PUBLIC_ID.toString(), new BigDecimal("100.00"), "x", 9L);

		assertEquals(RefundStatus.COMPLETED, refund.getStatus());
	}

	@Test
	void gatewayFailure_marksTheReservationFailed_andNothingStaysReserved() {
		payment(true);
		alreadyRefunded("0");
		ResolvedPaymentGatewayConfig config = mock(ResolvedPaymentGatewayConfig.class);
		when(paymentGatewayResolver.resolve(1L)).thenReturn(Optional.of(config));
		when(paymentGatewayCredentialsDecryptor.decrypt(config)).thenReturn(mock(PaymentGatewayCredentials.class));
		when(paymentGatewayProviderRegistry.resolve(any())).thenReturn(provider);
		when(provider.refund(any(), anyString(), any())).thenThrow(new IllegalStateException("timeout"));

		assertThrows(BusinessException.class,
				() -> service.refund(PAYMENT_PUBLIC_ID.toString(), new BigDecimal("100.00"), "x", 9L));

		assertEquals(RefundStatus.FAILED, lastSaved.getStatus());
		assertFalse(lastSaved.isReserving());
	}

	@Test
	void unknownPayment_throwsNotFound() {
		when(feePaymentRepository.findByPublicIdAndTenantId(PAYMENT_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.refund(PAYMENT_PUBLIC_ID.toString(), new BigDecimal("1.00"), "x", 9L));
	}
}
