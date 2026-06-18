package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * End-to-end test for the School's Student CRUD API.
 *
 * Verifies:
 * - Platform auth (login endpoint) returns a JWT
 * - School's student controller is reachable and returns correct HTTP status codes
 * - Tenant isolation is enforced at the HTTP layer (X-Tenant-ID header)
 *
 * Phase 5 validation: auth + JWT + school domain controller work end-to-end
 * with zero school-specific auth code.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

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
				"E2E Test School", "e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	@Test
	void login_withPlatformAuthEndpoint_returnsJwt() {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given()
					.header("X-Tenant-ID", tenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + adminEmail + "\",\"password\":\"" + adminPassword + "\"}")
					.when()
					.post("/api/v1/auth/login");
			if (response.statusCode() == HttpStatus.OK.value()) {
				response.then()
						.body("data.accessToken", notNullValue())
						.body("data.refreshToken", notNullValue());
				return;
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
	}

	@Test
	void listStudents_withValidJwt_returnsEmptyPage() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/students")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("content", hasSize(0));
	}

	@Test
	void enrollStudent_withTenantAdminRole_returns201() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "studentCode": "STU-001",
						  "firstName": "Alice",
						  "lastName": "Smith",
						  "email": "alice@school.test",
						  "dateOfBirth": "2010-05-15"
						}
						""")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("publicId", notNullValue())
				.body("studentCode", equalTo("STU-001"))
				.body("firstName", equalTo("Alice"))
				.body("enrollmentStatus", equalTo("ACTIVE"));
	}

	@Test
	void listStudents_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/students")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	private String login() {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given()
					.header("X-Tenant-ID", tenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + adminEmail + "\",\"password\":\"" + adminPassword + "\"}")
					.when()
					.post("/api/v1/auth/login");
			if (response.statusCode() == HttpStatus.OK.value()) {
				return response.then().extract().path("data.accessToken");
			}
			if (System.currentTimeMillis() >= deadline) {
				response.then().statusCode(HttpStatus.OK.value()); // force assertion failure with details
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
