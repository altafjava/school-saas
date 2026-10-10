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
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation. See {@code ExamCrudE2ETest}'s class javadoc for why prerequisite IDs are
 * resolved via repository lookup rather than purely through HTTP.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeePaymentCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TestPublicIds publicIds;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private FeeStructureRepository feeStructureRepository;

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
				"FeePayment E2E School", "fp-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private Long createStudentId(String accessToken, String studentCode) {
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentCode\":\"" + studentCode + "\",\"firstName\":\"Alice\",\"lastName\":\"Smith\","
						+ "\"email\":\"alice-" + studentCode + "@school.test\",\"dateOfBirth\":\"2010-01-01\"}")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		return withTenant(() -> studentRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId).orElseThrow().getId());
	}

	private Long createFeeStructureId(String accessToken, String name) {
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"name\":\"" + name + "\",\"amount\":500.00,\"frequency\":\"MONTHLY\","
						+ "\"planType\":\"Standard\"}")
				.when()
				.post("/api/v1/fee-structures")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		return withTenant(() -> feeStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId).orElseThrow().getId());
	}

	private Long withTenant(java.util.function.Supplier<Long> action) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return action.get();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	@Test
	void recordFeePayment_asTenantAdmin_returns201() {
		String accessToken = login();
		Long studentId = createStudentId(accessToken, "STU-FP1");
		Long feeStructureId = createFeeStructureId(accessToken, "Tuition FP1");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId)
						+ "\",\"feeStructurePublicId\":\"" + publicIds.of(FeeStructure.class, feeStructureId)
						+ "\",\"paidAmount\":500.00,\"paidAt\":\"2026-02-01T10:00:00\","
						+ "\"receiptNumber\":\"RCPT-FP1\"}")
				.when()
				.post("/api/v1/fee-payments")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.receiptNumber", equalTo("RCPT-FP1"));
	}

	@Test
	void listFeePayments_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/fee-payments")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void recordFeePayment_asTeacherRole_returns403() {
		String accessToken = login();
		Long studentId = createStudentId(accessToken, "STU-FP2");
		Long feeStructureId = createFeeStructureId(accessToken, "Tuition FP2");
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId)
						+ "\",\"feeStructurePublicId\":\"" + publicIds.of(FeeStructure.class, feeStructureId)
						+ "\",\"paidAmount\":500.00,\"paidAt\":\"2026-02-01T10:00:00\","
						+ "\"receiptNumber\":\"RCPT-FP2\"}")
				.when()
				.post("/api/v1/fee-payments")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void feePaymentCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		Long studentId = createStudentId(accessToken, "STU-FP3");
		Long feeStructureId = createFeeStructureId(accessToken, "Tuition FP3");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId)
						+ "\",\"feeStructurePublicId\":\"" + publicIds.of(FeeStructure.class, feeStructureId)
						+ "\",\"paidAmount\":500.00,\"paidAt\":\"2026-02-01T10:00:00\","
						+ "\"receiptNumber\":\"RCPT-FP3\"}")
				.when()
				.post("/api/v1/fee-payments")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "fp-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/fee-payments/" + publicId)
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
