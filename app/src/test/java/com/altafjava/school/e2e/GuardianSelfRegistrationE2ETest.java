package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.GuardianRegistrationSettingsService;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.guardian.model.GuardianSelfRegistrationMode;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * {@code POST /api/v1/guardians/self-register} is {@code permitAll()} — reachable without a JWT
 * via a literal entry in platform-saas's {@code SecurityConfig} permitAll allowlist. The endpoint
 * itself performs no role or identity check of its own; its behavior is governed entirely by
 * {@link GuardianRegistrationSettingsService}'s per-tenant CLAIM_ONLY/OPEN mode.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GuardianSelfRegistrationE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private GuardianService guardianService;

	@Autowired
	private GuardianRegistrationSettingsService guardianRegistrationSettingsService;

	private Tenant tenant;
	private Long tenantId;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Guardian Self-Reg E2E School", "grd-sr-e2e-" + suffix, 1L, "admin-" + suffix + "@school.test",
				"Password123!", "USD"));
		tenantId = tenant.getId();
	}

	// guardianService.create() reads TenantContext.getCurrentTenantId() (ThreadLocal) on the
	// calling thread — the test thread here, distinct from the server's own request-handling
	// thread, which TenantContextFilter populates per HTTP request. Direct service calls used
	// purely for test setup therefore need this bracketing; HTTP calls via RestAssured do not.
	private void createPendingGuardian(Tenant forTenant, String firstName, String lastName, String email,
			String phone) {
		TenantContext.ForTesting.setCurrentTenant(forTenant.getId(), forTenant.getPublicId(),
				forTenant.getSubdomain(), forTenant.getType());
		try {
			guardianService.create(firstName, lastName, email, phone, null);
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	@Test
	void selfRegister_withoutJwt_reachesEndpointAndAppliesBusinessRules() {
		// No JWT and no pre-existing pending Guardian record — reaches the permitAll endpoint fine,
		// then correctly gets rejected by CLAIM_ONLY's business rule (400), not by auth (401).
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("{\"email\":\"anon@school.test\",\"password\":\"Password123!\","
						+ "\"firstName\":\"Anon\",\"lastName\":\"User\",\"phone\":\"555-0000\"}")
				.when()
				.post("/api/v1/guardians/self-register")
				.then()
				.statusCode(HttpStatus.CONFLICT.value());
	}

	@Test
	void selfRegister_claimingPendingGuardian_returns201() {
		String email = "pending-" + UUID.randomUUID() + "@school.test";
		createPendingGuardian(tenant, "Jane", "Doe", email, "+14155552671");
		String bearer = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + bearer)
				.contentType(ContentType.JSON)
				.body("{\"email\":\"" + email + "\",\"password\":\"Password123!\","
						+ "\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"phone\":\"+14155552671\"}")
				.when()
				.post("/api/v1/guardians/self-register")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.email", equalTo(email));
	}

	@Test
	void selfRegister_noPendingRecordDefaultMode_returns409() {
		String email = "unmatched-" + UUID.randomUUID() + "@school.test";
		String bearer = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + bearer)
				.contentType(ContentType.JSON)
				.body("{\"email\":\"" + email + "\",\"password\":\"Password123!\","
						+ "\"firstName\":\"Alex\",\"lastName\":\"Roe\",\"phone\":\"555-0200\"}")
				.when()
				.post("/api/v1/guardians/self-register")
				.then()
				.statusCode(HttpStatus.CONFLICT.value());
	}

	@Test
	void selfRegister_noPendingRecordOpenMode_returns201WithNewGuardian() {
		// Explicit-tenantId overload — no ThreadLocal TenantContext is set on this test thread
		// (only the server's own request-handling thread gets one, via TenantContextFilter).
		guardianRegistrationSettingsService.setMode(tenantId, GuardianSelfRegistrationMode.OPEN);
		String email = "open-" + UUID.randomUUID() + "@school.test";
		String bearer = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + bearer)
				.contentType(ContentType.JSON)
				.body("{\"email\":\"" + email + "\",\"password\":\"Password123!\","
						+ "\"firstName\":\"Alex\",\"lastName\":\"Roe\",\"phone\":\"+14155552671\"}")
				.when()
				.post("/api/v1/guardians/self-register")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.email", equalTo(email));
	}

	@Test
	void selfRegister_pendingGuardianUnderOtherTenant_isNotClaimedAcrossTenants() {
		String email = "cross-tenant-" + UUID.randomUUID() + "@school.test";
		createPendingGuardian(tenant, "Cross", "Tenant", email, "+14155552671");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		var otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "grd-sr-other-" + otherSuffix, 1L, "admin@" + otherSuffix + ".test",
				"Password123!", "USD"));
		String bearer = authHelper.tokenWithRole(otherTenant.getId(), "TEACHER");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + bearer)
				.contentType(ContentType.JSON)
				.body("{\"email\":\"" + email + "\",\"password\":\"Password123!\","
						+ "\"firstName\":\"Cross\",\"lastName\":\"Tenant\",\"phone\":\"555-0400\"}")
				.when()
				.post("/api/v1/guardians/self-register")
				.then()
				.statusCode(HttpStatus.CONFLICT.value());
	}
}
