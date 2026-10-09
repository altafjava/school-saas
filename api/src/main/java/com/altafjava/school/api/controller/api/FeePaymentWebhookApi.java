package com.altafjava.school.api.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fee Payment Webhook", description = "Inbound payment-gateway callback — permitAll(), since a gateway "
		+ "cannot present a JWT. Trust is established via gateway signature verification instead of "
		+ "@PreAuthorize; the tenant is resolved from the path, not a caller-supplied header.")
public interface FeePaymentWebhookApi {

	@Operation(summary = "Handle webhook", description = "Verifies the gateway's signature header and applies the payment event to the "
			+ "corresponding fee payment: 404 if the tenant or its gateway config can't be resolved, "
			+ "400 if the signature is invalid, otherwise the event is recorded against that tenant.")
	public ResponseEntity<Void> handleWebhook(
			@PathVariable String tenantPublicId,
			@PathVariable String providerType,
			@RequestBody String rawBody,
			@RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader);
}
