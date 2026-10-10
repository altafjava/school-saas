package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
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
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation.
 *
 * <p>
 * Note: {@code CreateTermRequest.academicYearId} requires the academic year's raw internal ID,
 * the same pre-existing gap {@code ExamCrudE2ETest} documents for other cross-entity references
 * — resolved here via direct repository lookup.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportCardE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TestPublicIds publicIds;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private AcademicYearRepository academicYearRepository;

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
				"Report Card E2E School", "rc-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String createStudent(String accessToken, String suffix) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentCode\":\"STU-RC-" + suffix + "\",\"firstName\":\"Alice\",\"lastName\":\"Smith\","
						+ "\"email\":\"alice-" + suffix + "@school.test\",\"dateOfBirth\":\"2010-01-01\"}")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String createTerm(String accessToken, String suffix) {
		String academicYearPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"name\":\"AY-" + suffix + "\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\","
						+ "\"current\":false}")
				.when()
				.post("/api/v1/academic-years")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		Long academicYearId;
		try {
			academicYearId = academicYearRepository
					.findByPublicIdAndTenantId(UUID.fromString(academicYearPublicId), tenantId).orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}

		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"name\":\"Term-" + suffix + "\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-06-30\","
						+ "\"academicYearPublicId\":\"" + publicIds.of(AcademicYear.class, academicYearId) + "\"}")
				.when()
				.post("/api/v1/terms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	@Test
	void generateReportCard_asTenantAdmin_returns201() {
		String accessToken = login();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		String studentPublicId = createStudent(accessToken, suffix);
		String termPublicId = createTerm(accessToken, suffix);

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.post("/api/v1/students/" + studentPublicId + "/report-cards?termPublicId=" + termPublicId)
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue());
	}

	@Test
	void listReportCards_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.when()
				.get("/api/v1/students/" + UUID.randomUUID() + "/report-cards")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void generateReportCard_asTeacherRole_returns403() {
		String accessToken = login();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		String studentPublicId = createStudent(accessToken, suffix);
		String termPublicId = createTerm(accessToken, suffix);
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.when()
				.post("/api/v1/students/" + studentPublicId + "/report-cards?termPublicId=" + termPublicId)
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void reportCardCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		String studentPublicId = createStudent(accessToken, suffix);
		String termPublicId = createTerm(accessToken, suffix);
		String reportCardPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.post("/api/v1/students/" + studentPublicId + "/report-cards?termPublicId=" + termPublicId)
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "rc-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/students/" + studentPublicId + "/report-cards/" + reportCardPublicId + "/download")
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
