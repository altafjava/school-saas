package com.altafjava.school.api.controller;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayCredentialsDecryptor;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayProviderRegistry;
import com.altafjava.platform.application.paymentgateway.PaymentGatewayResolver;
import com.altafjava.platform.application.paymentgateway.ResolvedPaymentGatewayConfig;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantContextSnapshot;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayCredentials;
import com.altafjava.platform.domain.paymentgateway.service.PaymentGatewayProvider;
import com.altafjava.platform.domain.paymentgateway.service.PaymentWebhookEvent;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.api.controller.api.FeePaymentWebhookApi;
import com.altafjava.school.application.service.FeeOnlinePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Per-tenant gateway webhook callback — {@code tenantPublicId} in the path is how a single, shared
 * webhook-receiving deployment routes an inbound callback to the right tenant's own gateway
 * account/credentials/secret, since (unlike the platform's own single-account
 * {@code /api/v1/billing/webhooks/stripe}) every school tenant here has its own gateway account.
 * <p>
 * {@code permitAll()} — no JWT is available for an inbound gateway callback. Reachable without a
 * JWT via a generic {@code /api/v1/*}{@code /webhooks/**} ant pattern in platform-saas's
 * {@code SecurityConfig} permitAll list (domain-neutral, so any future domain consumer's own
 * webhook endpoint benefits too, not just this one).
 * <p>
 * The raw body is read as a {@code String} — matching platform's own
 * {@code com.altafjava.platform.api.rest.billing.WebhookController} exactly — because signature
 * verification needs the exact original bytes; letting Spring deserialize to a DTO first would
 * verify against a re-serialized (and potentially different) payload.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fee-payments/webhooks")
@RequiredArgsConstructor
public class FeePaymentWebhookController implements FeePaymentWebhookApi {

	private final TenantRepository tenantRepository;
	private final PaymentGatewayResolver paymentGatewayResolver;
	private final PaymentGatewayProviderRegistry paymentGatewayProviderRegistry;
	private final PaymentGatewayCredentialsDecryptor paymentGatewayCredentialsDecryptor;
	private final FeeOnlinePaymentService feeOnlinePaymentService;

	// Not ApiResponse-wrapped, unlike every other endpoint in this codebase — matches platform's
	// own WebhookController: a gateway provider posts here and expects a raw HTTP response, not
	// this application's JSON envelope.
	@Override
	@PostMapping("/{tenantPublicId}/{providerType}")
	public ResponseEntity<Void> handleWebhook(
			@PathVariable String tenantPublicId,
			@PathVariable String providerType,
			@RequestBody String rawBody,
			@RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {
		Optional<Tenant> tenant = resolveTenant(tenantPublicId);
		if (tenant.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Optional<ResolvedPaymentGatewayConfig> resolvedConfig = paymentGatewayResolver.resolve(tenant.get().getId());
		if (resolvedConfig.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		PaymentGatewayCredentials credentials = paymentGatewayCredentialsDecryptor.decrypt(resolvedConfig.get());
		PaymentGatewayProvider provider = paymentGatewayProviderRegistry.resolve(resolvedConfig.get().providerType());

		if (!provider.verifyWebhookSignature(credentials, rawBody, signatureHeader)) {
			log.warn("action=fee_payment_webhook_signature_invalid tenant_id={} provider={}", tenant.get().getId(),
					providerType);
			return ResponseEntity.badRequest().build();
		}

		TenantContextSnapshot snapshot = new TenantContextSnapshot(
				tenant.get().getId(), tenant.get().getPublicId(), tenant.get().getSubdomain(), tenant.get().getType(),
				tenant.get().getOrganizationId());
		TenantContext.runAsTenant(snapshot, () -> {
			PaymentWebhookEvent event = provider.parseWebhookEvent(credentials, rawBody);
			feeOnlinePaymentService.recordFromWebhookEvent(event);
		});

		return ResponseEntity.ok().build();
	}

	private Optional<Tenant> resolveTenant(String tenantPublicId) {
		try {
			return tenantRepository.findByPublicId(UUID.fromString(tenantPublicId));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
