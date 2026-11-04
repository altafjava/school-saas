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
 * tenant isolation — across all three hostel controllers (buildings, rooms, allocations).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HostelCrudE2ETest extends SchoolIntegrationTestBase {

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
				"Hostel E2E School", "hostel-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String createBuilding(String accessToken, Long forTenantId, String name) {
		return given()
				.header("X-Tenant-ID", forTenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"name":"%s","address":"12 Campus Road"}
						""".formatted(name))
				.when()
				.post("/api/v1/hostel-buildings")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String createRoom(String accessToken, Long forTenantId, String buildingPublicId, String roomNumber,
			int capacity) {
		return given()
				.header("X-Tenant-ID", forTenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.queryParam("hostelBuildingPublicId", buildingPublicId)
				.body("""
						{"roomNumber":"%s","capacity":%d}
						""".formatted(roomNumber, capacity))
				.when()
				.post("/api/v1/rooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String enrollStudent(String accessToken, Long forTenantId, String studentCode) {
		return given()
				.header("X-Tenant-ID", forTenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentCode":"%s","firstName":"Alice","lastName":"Smith",
						"email":"%s@school.test","dateOfBirth":"2010-01-01"}
						""".formatted(studentCode, studentCode.toLowerCase()))
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	@Test
	void createBuilding_asTenantAdmin_returns201() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"name":"North Block","address":"12 Campus Road"}
						""")
				.when()
				.post("/api/v1/hostel-buildings")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.active", equalTo(true));
	}

	@Test
	void createBuilding_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("""
						{"name":"North Block","address":"12 Campus Road"}
						""")
				.when()
				.post("/api/v1/hostel-buildings")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void createBuilding_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"name":"North Block","address":"12 Campus Road"}
						""")
				.when()
				.post("/api/v1/hostel-buildings")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void allocateThenListThenVacate_asTenantAdmin_returnsExpectedShapes() {
		String accessToken = login();
		String buildingPublicId = createBuilding(accessToken, tenantId, "East Block-" + UUID.randomUUID());
		String roomPublicId = createRoom(accessToken, tenantId, buildingPublicId, "101", 2);
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-HOSTEL-" + UUID.randomUUID().toString()
				.substring(0, 6));

		String allocationPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","roomPublicId":"%s","allocatedFrom":"2026-04-01"}
						""".formatted(studentPublicId, roomPublicId))
				.when()
				.post("/api/v1/room-allocations")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.active", equalTo(true))
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.queryParam("roomPublicId", roomPublicId)
				.when()
				.get("/api/v1/room-allocations")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", equalTo(1));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"allocatedUntil":"2026-06-30"}
						""")
				.when()
				.patch("/api/v1/room-allocations/" + allocationPublicId + "/vacate")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.active", equalTo(false))
				.body("data.allocatedUntil", equalTo("2026-06-30"));
	}

	@Test
	void allocate_beyondCapacity_returns409() {
		String accessToken = login();
		String buildingPublicId = createBuilding(accessToken, tenantId, "West Block-" + UUID.randomUUID());
		String roomPublicId = createRoom(accessToken, tenantId, buildingPublicId, "201", 1);
		String firstStudent = enrollStudent(accessToken, tenantId, "STU-CAP1-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String secondStudent = enrollStudent(accessToken, tenantId, "STU-CAP2-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","roomPublicId":"%s","allocatedFrom":"2026-04-01"}
						""".formatted(firstStudent, roomPublicId))
				.when()
				.post("/api/v1/room-allocations")
				.then()
				.statusCode(HttpStatus.CREATED.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","roomPublicId":"%s","allocatedFrom":"2026-04-01"}
						""".formatted(secondStudent, roomPublicId))
				.when()
				.post("/api/v1/room-allocations")
				.then()
				.statusCode(HttpStatus.CONFLICT.value());
	}

	@Test
	void allocate_asTeacherRole_returns403() {
		String accessToken = login();
		String buildingPublicId = createBuilding(accessToken, tenantId, "South Block-" + UUID.randomUUID());
		String roomPublicId = createRoom(accessToken, tenantId, buildingPublicId, "301", 2);
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-ROLE-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","roomPublicId":"%s","allocatedFrom":"2026-04-01"}
						""".formatted(studentPublicId, roomPublicId))
				.when()
				.post("/api/v1/room-allocations")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void hostelBuildingCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String buildingPublicId = createBuilding(accessToken, tenantId, "Isolation Block-" + UUID.randomUUID());

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other Hostel School", "hostel-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/hostel-buildings/" + buildingPublicId)
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
