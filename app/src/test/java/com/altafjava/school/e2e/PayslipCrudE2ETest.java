package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.time.YearMonth;
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
import com.altafjava.school.application.service.PayslipService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.payroll.model.Payslip;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation.
 *
 * <p>
 * There is no REST endpoint to generate a payslip on demand — generation is scheduler-only
 * ({@code PayslipGenerationJob}) — so payslips are seeded here via direct {@link PayslipService}
 * injection, matching CLAUDE.md's "direct repo injection for setup" testing convention.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PayslipCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private PayslipService payslipService;

	@Autowired
	private TeacherRepository teacherRepository;

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
				"Payslip E2E School", "pay-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String hireTeacher(String accessToken, String employeeCode) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "employeeCode": "%s",
						  "firstName": "Jane",
						  "lastName": "Doe",
						  "email": "%s@school.test",
						  "joinDate": "2020-08-01"
						}
						""".formatted(employeeCode, employeeCode.toLowerCase()))
				.when()
				.post("/api/v1/teachers")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private void createSalaryStructure(String accessToken, String teacherPublicId) {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "employeePublicId": "%s",
						  "components": [
						    {"code": "BASIC", "amount": 50000.00},
						    {"code": "HRA", "amount": 10000.00},
						    {"code": "TRANSPORT", "amount": 2000.00},
						    {"code": "OTHER_ALLOWANCE", "amount": 500.00},
						    {"code": "OTHER_DEDUCTION", "amount": 1000.00}
						  ],
						  "effectiveFrom": "2026-01-01"
						}
						""".formatted(teacherPublicId))
				.when()
				.post("/api/v1/salary-structures")
				.then()
				.statusCode(HttpStatus.CREATED.value());
	}

	private String generatePayslip(String teacherPublicId, YearMonth payMonth) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			Long teacherId = teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
					.orElseThrow().getId();
			Payslip payslip = payslipService.generate(teacherId, payMonth);
			return payslip.getPublicId().toString();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String seedPayslip(String accessToken, String employeeCode) {
		String teacherPublicId = hireTeacher(accessToken, employeeCode);
		createSalaryStructure(accessToken, teacherPublicId);
		return generatePayslip(teacherPublicId, YearMonth.of(2026, 5));
	}

	@Test
	void listPayslips_asTenantAdmin_returnsSeededPayslip() {
		String accessToken = login();
		seedPayslip(accessToken, "EMP-PAY-1");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/payslips")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", equalTo(1))
				.body("data.content[0].status", equalTo("DRAFT"))
				.body("data.content[0].publicId", notNullValue());
	}

	@Test
	void finalizeThenDisburse_transitionsThroughStatuses() {
		String accessToken = login();
		String publicId = seedPayslip(accessToken, "EMP-PAY-2");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.patch("/api/v1/payslips/" + publicId + "/finalize")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("FINALIZED"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.patch("/api/v1/payslips/" + publicId + "/disburse")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("DISBURSED"));
	}

	@Test
	void disburse_beforeFinalize_returns400() {
		String accessToken = login();
		String publicId = seedPayslip(accessToken, "EMP-PAY-3");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.patch("/api/v1/payslips/" + publicId + "/disburse")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void listPayslips_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.when()
				.get("/api/v1/payslips")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void listPayslips_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.when()
				.get("/api/v1/payslips")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void disburse_asHrRole_returns403() {
		String accessToken = login();
		String publicId = seedPayslip(accessToken, "EMP-PAY-4");
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.patch("/api/v1/payslips/" + publicId + "/finalize")
				.then()
				.statusCode(HttpStatus.OK.value());

		String hrToken = authHelper.tokenWithRole(tenantId, "HR");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + hrToken)
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.patch("/api/v1/payslips/" + publicId + "/disburse")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void payslipCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String publicId = seedPayslip(accessToken, "EMP-PAY-5");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "pay-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/payslips/" + publicId)
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
