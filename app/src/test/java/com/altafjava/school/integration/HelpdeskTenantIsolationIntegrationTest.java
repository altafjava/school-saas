package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.TicketService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.helpdesk.model.Ticket;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;

/**
 * Verifies that a ticket raised under tenant A is not visible or actionable from tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class HelpdeskTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private TicketService ticketService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"Helpdesk School A", "helpdesk-a-" + suffix, 1L, "admin@helpdesk-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Helpdesk School B", "helpdesk-b-" + suffix, 1L, "admin@helpdesk-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	@Test
	void ticketRaisedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		authenticateAsUser(55L);
		Ticket ticket = ticketService.raise(TicketCategory.TECHNICAL, "Cannot log in", "Blank screen on login");
		String ticketPublicId = ticket.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> ticketService.get(ticketPublicId),
				"Tenant B must not be able to resolve tenant A's ticket");
	}

	@Test
	void ticketsRaisedUnderTenantA_notCountedInTenantBSearch() {
		activateTenant(tenantA);
		authenticateAsUser(55L);
		ticketService.raise(TicketCategory.FEE, "Fee discrepancy", "Amount charged is higher than the fee structure");

		activateTenant(tenantB);
		var results = ticketService.search(null, null, null, null, PageRequest.of(0, 20));
		assertTrue(results.isEmpty(), "Tenant B's search must not include tenant A's tickets");
	}
}
