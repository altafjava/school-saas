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
 * tenant isolation — across the custom-field-definition CRUD controller and the student
 * custom-field value endpoints.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomFieldE2ETest extends SchoolIntegrationTestBase {

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
				"CustomField E2E School", "cf-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String defineField(String accessToken, Long forTenantId, String fieldKey, String fieldType,
			boolean required) {
		return given()
				.header("X-Tenant-ID", forTenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"entityType":"STUDENT","fieldKey":"%s","label":"%s","fieldType":"%s","required":%s}
						""".formatted(fieldKey, fieldKey, fieldType, required))
				.when()
				.post("/api/v1/custom-field-definitions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String defineSelectField(String accessToken, Long forTenantId, String fieldKey, String options) {
		return given()
				.header("X-Tenant-ID", forTenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"entityType":"STUDENT","fieldKey":"%s","label":"%s","fieldType":"SELECT","required":false,
						"validationRule":{"options":[%s]}}
						""".formatted(fieldKey, fieldKey, options))
				.when()
				.post("/api/v1/custom-field-definitions")
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
						{"studentCode":"%s","firstName":"Dana","lastName":"Lee",
						"email":"%s@school.test","dateOfBirth":"2011-05-05"}
						""".formatted(studentCode, studentCode.toLowerCase()))
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	@Test
	void defineField_asTenantAdmin_returns201() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"entityType":"STUDENT","fieldKey":"nickname","label":"Nickname","fieldType":"TEXT","required":false}
						""")
				.when()
				.post("/api/v1/custom-field-definitions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.active", equalTo(true));
	}

	@Test
	void defineField_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("""
						{"entityType":"STUDENT","fieldKey":"nickname","label":"Nickname","fieldType":"TEXT","required":false}
						""")
				.when()
				.post("/api/v1/custom-field-definitions")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void defineField_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"entityType":"STUDENT","fieldKey":"nickname","label":"Nickname","fieldType":"TEXT","required":false}
						""")
				.when()
				.post("/api/v1/custom-field-definitions")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void setThenGetValue_asTenantAdmin_roundTripsSuccessfully() {
		String accessToken = login();
		defineField(accessToken, tenantId, "bloodGroup", "TEXT", false);
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CF-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"values":{"bloodGroup":"O+"}}
						""")
				.when()
				.put("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.find { it.fieldKey == 'bloodGroup' }.value", equalTo("O+"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.find { it.fieldKey == 'bloodGroup' }.value", equalTo("O+"));
	}

	@Test
	void setValue_forUndefinedField_returns400() {
		String accessToken = login();
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CF2-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"values":{"doesNotExist":"x"}}
						""")
				.when()
				.put("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void setValue_forInactiveField_returns400() {
		String accessToken = login();
		String definitionPublicId = defineField(accessToken, tenantId, "retiredField", "TEXT", false);
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.patch("/api/v1/custom-field-definitions/" + definitionPublicId + "/deactivate")
				.then()
				.statusCode(HttpStatus.OK.value());
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CF3-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"values":{"retiredField":"x"}}
						""")
				.when()
				.put("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void setValue_withTypeMismatch_returns400() {
		String accessToken = login();
		defineField(accessToken, tenantId, "heightCm", "NUMBER", false);
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CF4-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"values":{"heightCm":"tall"}}
						""")
				.when()
				.put("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void setValue_asTeacherRole_returns403() {
		String accessToken = login();
		defineField(accessToken, tenantId, "notes", "TEXT", false);
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CF5-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"values":{"notes":"x"}}
						""")
				.when()
				.put("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void getValues_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.when()
				.get("/api/v1/custom-fields/STUDENT/" + UUID.randomUUID())
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void customFieldDefinedUnderOneTenant_notUsableFromAnotherTenant() {
		String accessToken = login();
		defineField(accessToken, tenantId, "isolationField", "TEXT", false);
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CF6-" + UUID.randomUUID().toString()
				.substring(0, 6));

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other CF School", "cf-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		// Tenant B never defined "isolationField" — setting it must fail as unknown, not silently
		// resolve tenant A's definition.
		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"values":{"isolationField":"x"}}
						""")
				.when()
				.put("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void defineField_selectTypeWithoutOptions_returns400() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"entityType":"STUDENT","fieldKey":"houseColor","label":"House Color","fieldType":"SELECT",
						"required":false}
						""")
				.when()
				.post("/api/v1/custom-field-definitions")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void setThenGetValue_selectTypeWithValidOption_roundTripsSuccessfully() {
		String accessToken = login();
		defineSelectField(accessToken, tenantId, "houseColor", "\"Red\",\"Blue\",\"Green\"");
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CF7-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"values":{"houseColor":"Blue"}}
						""")
				.when()
				.put("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.find { it.fieldKey == 'houseColor' }.value", equalTo("Blue"))
				.body("data.find { it.fieldKey == 'houseColor' }.options", equalTo(java.util.List.of("Red", "Blue",
						"Green")));
	}

	@Test
	void setValue_selectTypeWithOptionNotInList_returns400() {
		String accessToken = login();
		defineSelectField(accessToken, tenantId, "houseColor2", "\"Red\",\"Blue\"");
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CF8-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"values":{"houseColor2":"Purple"}}
						""")
				.when()
				.put("/api/v1/custom-fields/STUDENT/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
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
