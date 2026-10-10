package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import java.nio.charset.StandardCharsets;
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
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403.
 * Tenant isolation doesn't apply here beyond what StudentCrudE2ETest already covers — a bulk
 * import always writes into the caller's own tenant, there is no cross-tenant read path.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentBulkImportE2ETest extends SchoolIntegrationTestBase {

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
				"Bulk Import E2E School", "bulk-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	@Test
	void bulkImport_asTenantAdmin_returns200WithPerRowResults() {
		String accessToken = login();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		String csv = """
				studentCode,firstName,lastName,email,dateOfBirth
				STU-BI-A-%s,Alice,Smith,alice-%s@school.test,2010-01-15
				STU-BI-B-%s,,Jones,bob-%s@school.test,2011-02-20
				""".formatted(suffix, suffix, suffix, suffix);

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.multiPart("file", "students.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/students/bulk-import")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.totalRows", equalTo(2))
				.body("data.successCount", equalTo(1))
				.body("data.failureCount", equalTo(1));
	}

	@Test
	void bulkImport_withA2MbFile_isAccepted() {
		String accessToken = login();
		// A blank first name fails the row cheaply, so the 2 MB body exercises the multipart limit, not the importer.
		String csv = "studentCode,firstName,lastName,email,dateOfBirth\nSTU-BIG,," + "x".repeat(2 * 1024 * 1024)
				+ ",big@school.test,2010-01-15\n";

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.multiPart("file", "students.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.when()
				.post("/api/v1/students/bulk-import")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.totalRows", equalTo(1));
	}

	@Test
	void bulkImport_overTheUploadLimit_returns413PayloadTooLarge() {
		String accessToken = login();
		byte[] overTheLimit = new byte[10 * 1024 * 1024 + 1];

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.multiPart("file", "students.csv", overTheLimit, "text/csv")
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.when()
				.post("/api/v1/students/bulk-import")
				.then()
				.statusCode(HttpStatus.CONTENT_TOO_LARGE.value())
				.body("error.code", equalTo("PAYLOAD_TOO_LARGE"));
	}

	@Test
	void bulkImport_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.multiPart("file", "students.csv", "studentCode,firstName,lastName,email,dateOfBirth\n"
						.getBytes(StandardCharsets.UTF_8), "text/csv")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/students/bulk-import")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void bulkImport_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.multiPart("file", "students.csv", "studentCode,firstName,lastName,email,dateOfBirth\n"
						.getBytes(StandardCharsets.UTF_8), "text/csv")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/students/bulk-import")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
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
