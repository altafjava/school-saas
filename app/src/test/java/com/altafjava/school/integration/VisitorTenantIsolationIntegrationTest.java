package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.application.service.VisitorLogService;
import com.altafjava.school.application.service.VisitorRequestService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.visitor.model.VisitorLog;
import com.altafjava.school.domain.visitor.model.VisitorRequest;
import com.altafjava.school.util.TestPhotos;

/**
 * Verifies that visit requests and visitor logs created under tenant A are not visible or
 * actionable from tenant B, and that the approval and double-checkout guards hold through a real
 * database round trip.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
class VisitorTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	private static final Long ACTOR = -1L;

	@Autowired
	private VisitorLogService visitorLogService;

	@Autowired
	private VisitorRequestService visitorRequestService;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private DocumentIssuanceService documentIssuanceService;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private TestPhotos testPhotos;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"Visitor School A", "visitor-a-" + suffix, 1L, "admin@visitor-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Visitor School B", "visitor-b-" + suffix, 1L, "admin@visitor-b.test", "Password123!", "USD"));
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
		AuthenticatedUser principal = new AuthenticatedUser() {
			@Override
			public Long getId() {
				return ACTOR;
			}

			@Override
			public String getUsername() {
				return "front-desk";
			}

			@Override
			public Long getTenantId() {
				return null;
			}
		};
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	private VisitorRequest approvedWalkIn(Tenant tenant, String hostEmail) {
		Teacher host = teacherService.hire("EMP-" + UUID.randomUUID().toString().substring(0, 6), "Jane", "Doe",
				hostEmail, LocalDate.of(2020, 1, 1));
		VisitorRequest request = visitorRequestService.raise("Alex Ray", "555-0100", "Parent-teacher meeting",
				host.getPublicId().toString(), null, ACTOR);
		return visitorRequestService.approve(request.getPublicId().toString(), ACTOR);
	}

	private VisitorLog checkInVisitor(Tenant tenant, String hostEmail) {
		VisitorRequest approved = approvedWalkIn(tenant, hostEmail);
		UUID photo = testPhotos.store(tenant.getId());
		activateTenant(tenant);
		return visitorLogService.checkIn(approved.getPublicId().toString(), photo.toString(), ACTOR);
	}

	@Test
	void visitorLogCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		VisitorLog log = checkInVisitor(tenantA, "jane@visitor.test");
		String logPublicId = log.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> visitorLogService.findByPublicId(logPublicId),
				"Tenant B must not be able to resolve tenant A's visitor log");
	}

	@Test
	void visitorRequestCreatedUnderTenantA_notReachableFromTenantB() {
		activateTenant(tenantA);
		VisitorRequest request = approvedWalkIn(tenantA, "jane-req@visitor.test");
		String requestPublicId = request.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> visitorRequestService.findByPublicId(requestPublicId, ACTOR));
		assertThrows(ResourceNotFoundException.class, () -> visitorRequestService.approve(requestPublicId, ACTOR));
		assertThrows(ResourceNotFoundException.class,
				() -> visitorLogService.checkIn(requestPublicId, UUID.randomUUID().toString(), ACTOR),
				"Tenant B must not be able to admit a visitor on tenant A's approval");
	}

	@Test
	void checkIn_aRequestThatWasNeverApproved_isRefused() {
		activateTenant(tenantA);
		Teacher host = teacherService.hire("EMP-" + UUID.randomUUID().toString().substring(0, 6), "Jane", "Doe",
				"jane-pending@visitor.test", LocalDate.of(2020, 1, 1));
		VisitorRequest pending = visitorRequestService.raise("Sam Fox", "555-0200", "Vendor delivery",
				host.getPublicId().toString(), null, ACTOR);
		UUID photo = testPhotos.store(tenantA.getId());
		activateTenant(tenantA);

		assertThrows(BusinessException.class,
				() -> visitorLogService.checkIn(pending.getPublicId().toString(), photo.toString(), ACTOR));
	}

	@Test
	void checkIn_twiceOnOneApproval_isRefusedAndCheckOutRevokesTheBadge() {
		activateTenant(tenantA);
		VisitorRequest approved = approvedWalkIn(tenantA, "jane-twice@visitor.test");
		UUID photo = testPhotos.store(tenantA.getId());
		activateTenant(tenantA);
		VisitorLog log = visitorLogService.checkIn(approved.getPublicId().toString(), photo.toString(), ACTOR);
		String requestPublicId = approved.getPublicId().toString();

		assertThrows(BusinessException.class,
				() -> visitorLogService.checkIn(requestPublicId, photo.toString(), ACTOR),
				"One approval admits one visit");

		assertTrue(!documentIssuanceService.findById(log.getBadgeIssuanceId()).orElseThrow().isRevoked());
		VisitorLog checkedOut = visitorLogService.checkOut(log.getPublicId().toString());
		assertTrue(checkedOut.getCheckOutAt() != null);
		assertTrue(documentIssuanceService.findById(log.getBadgeIssuanceId()).orElseThrow().isRevoked());
		assertThrows(BusinessException.class, () -> visitorLogService.checkOut(log.getPublicId().toString()),
				"A visitor already checked out must not be checked out again");
	}
}
