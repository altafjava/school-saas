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
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TicketCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	private Long tenantId;
	private String adminEmail;
	private String adminPassword;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		adminEmail = "admin-" + suffix + "@school.test";
		adminPassword = "Password123!";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Ticket E2E School", "ticket-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	@Test
	void raiseTicket_asParentRole_returns201() {
		String parentToken = authHelper.tokenForUser(tenantId, 501L, "parent-" + UUID.randomUUID() + "@school.test",
				"PARENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + parentToken)
				.contentType(ContentType.JSON)
				.body("""
						{"category":"FEE","subject":"Fee discrepancy","description":"Charged amount is wrong"}
						""")
				.when()
				.post("/api/v1/tickets")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.status", equalTo("OPEN"));
	}

	@Test
	void raiseTicket_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("""
						{"category":"TECHNICAL","subject":"Cannot log in","description":"Blank screen"}
						""")
				.when()
				.post("/api/v1/tickets")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void searchTickets_asParentRole_returns403() {
		String parentToken = authHelper.tokenWithRole(tenantId, "PARENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + parentToken)
				.when()
				.get("/api/v1/tickets")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void raiseThenAssignThenResolveThenClose_asTenantAdmin_returnsExpectedShapes() {
		String accessToken = login();

		String ticketPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"category":"TECHNICAL","subject":"Cannot log in","description":"Blank screen on login"}
						""")
				.when()
				.post("/api/v1/tickets")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String assigneePublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/me")
				.then()
				.statusCode(HttpStatus.OK.value())
				.extract().path("data.user.id");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"assignedToUserPublicId\":\"" + assigneePublicId + "\"}")
				.when()
				.patch("/api/v1/tickets/" + ticketPublicId + "/assign")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("IN_PROGRESS"))
				.body("data.assignedToUserPublicId", equalTo(assigneePublicId));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"resolution":"Reset the password"}
						""")
				.when()
				.patch("/api/v1/tickets/" + ticketPublicId + "/resolve")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("RESOLVED"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.patch("/api/v1/tickets/" + ticketPublicId + "/close")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("CLOSED"));
	}

	@Test
	void ticketRaisedUnderOneTenant_returns404ForAnotherTenant() {
		String parentToken = authHelper.tokenForUser(tenantId, 502L, "parent2-" + UUID.randomUUID() + "@school.test",
				"PARENT");
		String ticketPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + parentToken)
				.contentType(ContentType.JSON)
				.body("""
						{"category":"OTHER","subject":"Isolation check","description":"Cross-tenant check"}
						""")
				.when()
				.post("/api/v1/tickets")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other Ticket School", "ticket-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/tickets/" + ticketPublicId)
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	private String login() {
		return login(tenantId, adminEmail, adminPassword);
	}

	private String login(Long forTenantId, String email, String password) {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given()
					.header("X-Tenant-ID", forTenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
					.when()
					.post("/api/v1/auth/login");
			if (response.statusCode() == HttpStatus.OK.value()) {
				return response.then().extract().path("data.accessToken");
			}
			if (System.currentTimeMillis() >= deadline) {
				response.then().statusCode(HttpStatus.OK.value());
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		throw new IllegalStateException("login timed out");
	}
}
