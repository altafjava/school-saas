package com.altafjava.school.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.event.events.TenantCreatedEvent;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.integration.SchoolTenantProvisioningIntegrationTest.TenantCreatedEventCaptor;

/**
 * Verifies that SchoolTenantProvisioningListener correctly receives TenantCreatedEvent
 * after the platform provisions a new tenant.
 *
 * Phase 5 validation goal: confirm the platform event contract works end-to-end.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TenantCreatedEventCaptor.class })
class SchoolTenantProvisioningIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private TenantCreatedEventCaptor eventCaptor;

	@Test
	void tenantRegistration_schoolListenerReceivesTenantCreatedEvent() {
		// Given
		String subdomain = "school-" + UUID.randomUUID().toString().substring(0, 8);
		RegisterTenantCommand command = new RegisterTenantCommand(
				"Springfield Elementary", subdomain, 1L,
				"admin@springfield.school", "Password123!", "USD");

		// When
		Tenant tenant = onboardingService.registerTenant(command);
		assertNotNull(tenant.getId(), "Tenant must be persisted");

		// Then — AFTER_COMMIT event fires asynchronously
		await().atMost(Duration.ofSeconds(5)).until(eventCaptor::received);

		assertTrue(eventCaptor.received(), "TenantCreatedEvent must have been received by school listener");
		assertTrue(eventCaptor.capturedTenantId().equals(tenant.getId()),
				"Event tenantId must match the registered tenant");
		assertNotNull(eventCaptor.capturedTenantType(), "Event must carry tenantType");
	}

	@Component
	static class TenantCreatedEventCaptor {
		private final AtomicBoolean received = new AtomicBoolean(false);
		private volatile Long capturedTenantId;
		private volatile Object capturedTenantType;

		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void onTenantCreated(TenantCreatedEvent event) {
			this.capturedTenantId = event.tenantId();
			this.capturedTenantType = event.tenantType();
			this.received.set(true);
		}

		boolean received() {
			return received.get();
		}

		Long capturedTenantId() {
			return capturedTenantId;
		}

		Object capturedTenantType() {
			return capturedTenantType;
		}
	}
}
