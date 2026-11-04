package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.security.PasswordEncoder;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserStatus;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Fee receipt downloads for fee staff and for families: the per-controller minimum (happy path, 401,
 * 403, tenant isolation), the receipt staying the same document, and a bounced cheque losing it.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeeReceiptE2ETest extends SchoolIntegrationTestBase {

	private static final String PASSWORD = "Password123!";

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Long tenantId;
	private String adminToken;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String adminEmail = "admin-" + suffix + "@school.test";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Fee Receipt E2E School", "rcpt-e2e-" + suffix, 1L, adminEmail, PASSWORD, "INR"));
		tenantId = tenant.getId();
		adminToken = login(tenantId, adminEmail);
	}

	@Test
	void receipt_asFeeStaff_isAPdfThatStaysTheSameDocument() {
		String paymentPublicId = recordPayment(createStudent("STU-RC1"), "{\"paymentMode\":\"CASH\"");

		byte[] first = downloadAsAdmin(paymentPublicId);
		byte[] second = downloadAsAdmin(paymentPublicId);

		assertEquals("%PDF", new String(Arrays.copyOf(first, 4), StandardCharsets.US_ASCII));
		assertArrayEquals(first, second);
	}

	@Test
	void receipt_afterTheChequeBounces_isRefused() {
		String paymentPublicId = recordPayment(createStudent("STU-RC2"),
				"{\"paymentMode\":\"CHEQUE\",\"chequeNumber\":\"004512\",\"chequeBank\":\"State Bank\","
						+ "\"chequeDate\":\"2026-01-30\"");
		downloadAsAdmin(paymentPublicId);

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"reason\":\"Insufficient funds\",\"bouncedAt\":\"2026-02-05T11:00:00\"}")
				.when()
				.post("/api/v1/fee-payments/" + paymentPublicId + "/bounce")
				.then()
				.statusCode(HttpStatus.OK.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.when()
				.get("/api/v1/fee-payments/" + paymentPublicId + "/receipt")
				.then()
				.statusCode(HttpStatus.CONFLICT.value())
				.body("error.message", containsString("bounced"));
	}

	@Test
	void receipt_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.when()
				.get("/api/v1/fee-payments/" + UUID.randomUUID() + "/receipt")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void receipt_asTeacherRole_returns403() {
		String paymentPublicId = recordPayment(createStudent("STU-RC3"), "{\"paymentMode\":\"CASH\"");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + authHelper.tokenWithRole(tenantId, "TEACHER"))
				.when()
				.get("/api/v1/fee-payments/" + paymentPublicId + "/receipt")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void receipt_fromAnotherTenant_returns404() {
		String paymentPublicId = recordPayment(createStudent("STU-RC4"), "{\"paymentMode\":\"CASH\"");
		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		String otherEmail = "admin@" + otherSuffix + ".test";
		Tenant other = onboardingService.registerTenant(new RegisterTenantCommand("Other School",
				"rcpt-other-" + otherSuffix, 1L, otherEmail, PASSWORD, "INR"));

		given()
				.header("X-Tenant-ID", other.getId())
				.header("Authorization", "Bearer " + login(other.getId(), otherEmail))
				.when()
				.get("/api/v1/fee-payments/" + paymentPublicId + "/receipt")
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void selfServiceReceipt_forOwnPayment_isAPdf_andAnotherStudentsIsForbidden() {
		String ownStudent = createStudent("STU-RC5");
		String otherStudent = createStudent("STU-RC6");
		String ownPayment = recordPayment(ownStudent, "{\"paymentMode\":\"UPI\",\"transactionReference\":\"UPI-7781\"");
		String otherPayment = recordPayment(otherStudent, "{\"paymentMode\":\"CASH\"");
		String email = "student-rc5-" + UUID.randomUUID().toString().substring(0, 6) + "@school.test";
		Long userId = createStudentUser(email);
		linkStudentToUser(ownStudent, userId);
		String studentToken = authHelper.tokenForUser(tenantId, userId, email, "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.when()
				.get("/api/v1/fee-payments/self-service/" + ownPayment + "/receipt")
				.then()
				.statusCode(HttpStatus.OK.value())
				.header("Content-Type", equalTo("application/pdf"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.when()
				.get("/api/v1/fee-payments/self-service/" + otherPayment + "/receipt")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void selfServiceReceipt_asTenantAdminRole_returns403() {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.when()
				.get("/api/v1/fee-payments/self-service/" + UUID.randomUUID() + "/receipt")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	private byte[] downloadAsAdmin(String paymentPublicId) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.when()
				.get("/api/v1/fee-payments/" + paymentPublicId + "/receipt")
				.then()
				.statusCode(HttpStatus.OK.value())
				.header("Content-Type", equalTo("application/pdf"))
				.header("Content-Disposition", containsString("receipt-"))
				.extract().asByteArray();
	}

	private String createStudent(String studentCode) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentCode\":\"" + studentCode + "\",\"firstName\":\"Alice\",\"lastName\":\"Smith\","
						+ "\"email\":\"alice-" + studentCode + "@school.test\",\"dateOfBirth\":\"2010-01-01\"}")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String recordPayment(String studentPublicId, String instrumentJsonPrefix) {
		String feeStructurePublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"name\":\"Tuition " + UUID.randomUUID().toString().substring(0, 6)
						+ "\",\"amount\":500.00,\"frequency\":\"MONTHLY\",\"planType\":\"Standard\"}")
				.when()
				.post("/api/v1/fee-structures")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body(instrumentJsonPrefix + ",\"studentPublicId\":\"" + studentPublicId
						+ "\",\"feeStructurePublicId\":\"" + feeStructurePublicId
						+ "\",\"paidAmount\":1500.00,\"paidAt\":\"2026-02-01T10:00:00\"}")
				.when()
				.post("/api/v1/fee-payments")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private Long createStudentUser(String email) {
		return withTenant(() -> {
			var role = roleRepository.findAll().stream()
					.filter(r -> r.getTenantId() == null && "STUDENT".equals(r.getName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("STUDENT role not seeded"));
			User user = User.builder()
					.email(email)
					.passwordHash(passwordEncoder.encode(PASSWORD))
					.status(UserStatus.ACTIVE)
					.emailVerified(true)
					.build();
			user.addRole(role);
			return userRepository.save(user).getId();
		});
	}

	private void linkStudentToUser(String studentPublicId, Long userId) {
		withTenant(() -> {
			var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
					.orElseThrow();
			student.setUserId(userId);
			return studentRepository.save(student);
		});
	}

	private <T> T withTenant(Supplier<T> action) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return action.get();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private static String login(Long forTenantId, String email) {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given()
					.header("X-Tenant-ID", forTenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")
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
				throw new IllegalStateException("login interrupted", e);
			}
		}
	}
}
