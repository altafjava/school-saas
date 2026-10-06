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
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation — across all three certificate controllers (templates, issuance, verification).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CertificateCrudE2ETest extends SchoolIntegrationTestBase {

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
				"Certificate E2E School", "cert-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private static String uniqueCode() {
		return "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
	}

	private String createTemplate(String accessToken, Long forTenantId, String name) {
		return given()
				.header("X-Tenant-ID", forTenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"code":"%s","name":"%s","wording":"This certifies {{studentName}} of {{className}}."}
						""".formatted(uniqueCode(), name))
				.when()
				.post("/api/v1/certificate-types")
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
	void createTemplate_asTenantAdmin_returns201() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"code":"BONAFIDE","name":"Bonafide Certificate","wording":"Certifies {{studentName}}."}
						""")
				.when()
				.post("/api/v1/certificate-types")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.active", equalTo(true));
	}

	@Test
	void createTemplate_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("""
						{"code":"BONAFIDE","name":"Bonafide Certificate","wording":"Certifies {{studentName}}."}
						""")
				.when()
				.post("/api/v1/certificate-types")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void createTemplate_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"code":"BONAFIDE","name":"Bonafide Certificate","wording":"Certifies {{studentName}}."}
						""")
				.when()
				.post("/api/v1/certificate-types")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void issueThenListThenDownload_asTenantAdmin_returnsExpectedShapes() {
		String accessToken = login();
		String templatePublicId = createTemplate(accessToken, tenantId, "Bonafide-" + UUID.randomUUID());
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CERT-" + UUID.randomUUID().toString()
				.substring(0, 6));

		String certificatePublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.post("/api/v1/students/" + studentPublicId + "/certificates?certificateTypePublicId="
						+ templatePublicId)
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.verificationCode", notNullValue())
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/students/" + studentPublicId + "/certificates")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", equalTo(1));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/students/" + studentPublicId + "/certificates/" + certificatePublicId + "/download")
				.then()
				.statusCode(HttpStatus.OK.value())
				.header("Content-Type", equalTo("application/pdf"));
	}

	@Test
	void issueCertificate_asTeacherRole_returns403() {
		String accessToken = login();
		String templatePublicId = createTemplate(accessToken, tenantId, "Transfer-" + UUID.randomUUID());
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CERT2-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.when()
				.post("/api/v1/students/" + studentPublicId + "/certificates?certificateTypePublicId="
						+ templatePublicId)
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void verifyIssuedCertificate_asAnyAuthenticatedRole_returnsMinimalConfirmation() {
		String accessToken = login();
		String templatePublicId = createTemplate(accessToken, tenantId, "Character-" + UUID.randomUUID());
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CERT3-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String verificationCode = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.post("/api/v1/students/" + studentPublicId + "/certificates?certificateTypePublicId="
						+ templatePublicId)
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.verificationCode");

		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.when()
				.get("/api/v1/documents/verify/" + verificationCode)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.subjectName", equalTo("Alice Smith"))
				.body("data.status", equalTo("VALID"))
				.body("data.issuedAt", notNullValue());
	}

	@Test
	void verifyUnknownCode_returns404() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/documents/verify/does-not-exist")
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void verifyIssuedCertificate_withoutJwt_returnsMinimalConfirmation() {
		String accessToken = login();
		String templatePublicId = createTemplate(accessToken, tenantId, "Bonafide-" + UUID.randomUUID());
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CERT4-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String verificationCode = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.post("/api/v1/students/" + studentPublicId + "/certificates?certificateTypePublicId="
						+ templatePublicId)
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.verificationCode");

		// No Authorization header at all — a third party with no account on this tenant, the
		// exact caller this endpoint exists for.
		given()
				.header("X-Tenant-ID", tenantId)
				.when()
				.get("/api/v1/documents/verify/" + verificationCode)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.subjectName", equalTo("Alice Smith"))
				.body("data.status", equalTo("VALID"));
	}

	@Test
	void verifyUnknownCode_withoutJwt_returns404() {
		given()
				.header("X-Tenant-ID", tenantId)
				.when()
				.get("/api/v1/documents/verify/does-not-exist")
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void certificateCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String templatePublicId = createTemplate(accessToken, tenantId, "Isolation-" + UUID.randomUUID());
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-CERT4-" + UUID.randomUUID().toString()
				.substring(0, 6));
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.post("/api/v1/students/" + studentPublicId + "/certificates?certificateTypePublicId="
						+ templatePublicId)
				.then()
				.statusCode(HttpStatus.CREATED.value());

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other Cert School", "cert-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/students/" + studentPublicId + "/certificates")
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
