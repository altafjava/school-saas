package com.altafjava.school.config;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayCredentialsDecryptor;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayProviderRegistry;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayResolver;
import com.altafjava.platform.application.paymentgateway.ResolvedPaymentGatewayConfig;
import com.altafjava.platform.core.exception.TechnicalException;
import com.altafjava.platform.domain.paymentgateway.model.PaymentGatewayConfirmationMode;
import com.altafjava.platform.domain.paymentgateway.model.PaymentGatewayType;
import com.altafjava.platform.domain.paymentgateway.service.PaymentChargeRequest;
import com.altafjava.platform.domain.paymentgateway.service.PaymentChargeResult;
import com.altafjava.platform.domain.paymentgateway.service.PaymentChargeStatus;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayCredentials;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayProvider;
import com.altafjava.platform.domain.paymentgateway.service.PaymentWebhookEvent;
import com.altafjava.platform.domain.paymentgateway.service.RefundResult;
import tools.jackson.databind.ObjectMapper;

/**
 * Test-only replacement for the platform's payment-gateway beans, mirroring
 * {@link TestPaymentConfig}'s exact shape (a hand-written stub implementation of the SPI, wired in
 * as {@code @Primary}) rather than mocking every call site individually — every school-saas test
 * that exercises {@code FeeOnlinePaymentService}/{@code FeePaymentWebhookController} gets a
 * deterministic, always-configured Stripe-type gateway without touching a real Stripe account.
 * <p>
 * {@link PaymentGatewayResolver} and {@link PaymentGatewayCredentialsDecryptor} are concrete
 * platform classes (not SPI interfaces), so they are stubbed via {@link Mockito#mock} instead —
 * the same pattern {@code TestRedisConfig} already uses for platform infrastructure types this
 * module doesn't own.
 */
@TestConfiguration
@Profile("test")
public class TestPaymentGatewayConfig {

	public static final String VALID_SIGNATURE = "valid-signature";
	/** Lets a test take the gateway down, because the failure path is what it needs to prove. */
	public static final AtomicBoolean GATEWAY_DOWN = new AtomicBoolean();

	@Bean
	@Primary
	public PaymentGatewayResolver paymentGatewayResolver() {
		PaymentGatewayResolver resolver = Mockito.mock(PaymentGatewayResolver.class);
		ResolvedPaymentGatewayConfig config = new ResolvedPaymentGatewayConfig(PaymentGatewayType.STRIPE, "sandbox",
				PaymentGatewayConfirmationMode.BOTH, "test-ciphertext");
		Mockito.when(resolver.resolve(Mockito.anyLong())).thenReturn(java.util.Optional.of(config));
		return resolver;
	}

	@Bean
	@Primary
	public PaymentGatewayCredentialsDecryptor paymentGatewayCredentialsDecryptor() {
		PaymentGatewayCredentialsDecryptor decryptor = Mockito.mock(PaymentGatewayCredentialsDecryptor.class);
		Mockito.when(decryptor.decrypt(Mockito.any())).thenReturn(
				new PaymentGatewayCredentials(PaymentGatewayType.STRIPE, "sandbox", Map.of()));
		return decryptor;
	}

	@Bean
	@Primary
	public PaymentGatewayProviderRegistry paymentGatewayProviderRegistry(ObjectMapper objectMapper) {
		return new PaymentGatewayProviderRegistry(java.util.List.of(new StubPaymentGatewayProvider(objectMapper)));
	}

	static class StubPaymentGatewayProvider implements PaymentGatewayProvider {

		private final Map<String, PaymentChargeRequest> charges = new ConcurrentHashMap<>();
		private final ObjectMapper objectMapper;

		StubPaymentGatewayProvider(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public PaymentGatewayType getType() {
			return PaymentGatewayType.STRIPE;
		}

		@Override
		public PaymentChargeResult createCharge(PaymentGatewayCredentials credentials, PaymentChargeRequest request) {
			if (GATEWAY_DOWN.get()) {
				throw new TechnicalException("Gateway unavailable", null);
			}
			String reference = "pi_test_" + UUID.randomUUID();
			charges.put(reference, request);
			return new PaymentChargeResult(reference, "requires_confirmation", "secret_" + reference);
		}

		@Override
		public PaymentChargeStatus getChargeStatus(PaymentGatewayCredentials credentials,
				String gatewayChargeReference) {
			PaymentChargeRequest request = charges.get(gatewayChargeReference);
			BigDecimal amount = request != null ? request.amount() : BigDecimal.ZERO;
			String currency = request != null ? request.currency() : "USD";
			return new PaymentChargeStatus(gatewayChargeReference, "succeeded", amount, currency);
		}

		@Override
		public RefundResult refund(PaymentGatewayCredentials credentials, String gatewayChargeReference,
				BigDecimal amount) {
			throw new UnsupportedOperationException("Not exercised by school-saas tests");
		}

		@Override
		public boolean verifyWebhookSignature(PaymentGatewayCredentials credentials, String rawPayload,
				String signatureHeader) {
			return VALID_SIGNATURE.equals(signatureHeader);
		}

		@Override
		@SuppressWarnings("unchecked")
		public PaymentWebhookEvent parseWebhookEvent(PaymentGatewayCredentials credentials, String rawPayload) {
			Map<String, Object> json = objectMapper.readValue(rawPayload, Map.class);
			String eventType = (String) json.getOrDefault("eventType", "payment_intent.succeeded");
			String reference = (String) json.get("gatewayChargeReference");
			String status = (String) json.getOrDefault("status", "succeeded");
			Map<String, String> metadata = (Map<String, String>) json.getOrDefault("metadata", Map.of());
			return new PaymentWebhookEvent(eventType, reference, status, metadata);
		}
	}
}
