package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
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
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

// Exercises TESTING.md §1's RBAC matrix for the Phase 1 student-scoped read endpoints.
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentDataAccessE2ETest extends SchoolIntegrationTestBase {

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
	private ExamRepository examRepository;

	@Autowired
	private FeeStructureRepository feeStructureRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Long tenantId;
	private String adminToken;
	private String studentAPublicId;
	private Long studentAId;
	private String studentBPublicId;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String adminEmail = "admin-" + suffix + "@school.test";
		String adminPassword = "Password123!";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Access E2E School", "access-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
		adminToken = login(adminEmail, adminPassword);

		studentAPublicId = createStudent("STU-ACC-A-" + suffix, "alice-" + suffix + "@school.test");
		studentAId = withTenant(() -> studentRepository
				.findByPublicIdAndTenantId(UUID.fromString(studentAPublicId), tenantId).orElseThrow().getId());
		studentBPublicId = createStudent("STU-ACC-B-" + suffix, "bob-" + suffix + "@school.test");
	}

	@Test
	void tenantAdmin_canViewAnyStudentsGrades() {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.when()
				.get("/api/v1/students/" + studentAPublicId + "/grades")
				.then()
				.statusCode(HttpStatus.OK.value());
	}

	@Test
	void parent_canViewOwnLinkedChildsGrades() {
		Long parentUserId = createUserWithRole("parent-" + UUID.randomUUID().toString().substring(0, 8)
				+ "@school.test", "PARENT");
		String guardianPublicId = createGuardian(parentUserId);
		linkGuardianToStudent(guardianPublicId, studentAPublicId);
		String parentToken = authHelper.tokenForUser(tenantId, parentUserId, "parent@school.test", "PARENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + parentToken)
				.when()
				.get("/api/v1/students/" + studentAPublicId + "/grades")
				.then()
				.statusCode(HttpStatus.OK.value());
	}

	@Test
	void parent_cannotViewAnotherStudentsGrades() {
		Long parentUserId = createUserWithRole("parent-" + UUID.randomUUID().toString().substring(0, 8)
				+ "@school.test", "PARENT");
		String guardianPublicId = createGuardian(parentUserId);
		linkGuardianToStudent(guardianPublicId, studentAPublicId);
		String parentToken = authHelper.tokenForUser(tenantId, parentUserId, "parent@school.test", "PARENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + parentToken)
				.when()
				.get("/api/v1/students/" + studentBPublicId + "/grades")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void student_canViewOwnAttendance() {
		Long studentUserId = createUserWithRole("student-" + UUID.randomUUID().toString().substring(0, 8)
				+ "@school.test", "STUDENT");
		withTenant(() -> {
			var student = studentRepository.findByIdAndTenantId(studentAId, tenantId).orElseThrow();
			student.setUserId(studentUserId);
			studentRepository.save(student);
			return null;
		});
		String studentToken = authHelper.tokenForUser(tenantId, studentUserId, "student@school.test", "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.when()
				.get("/api/v1/students/" + studentAPublicId + "/attendance")
				.then()
				.statusCode(HttpStatus.OK.value());
	}

	@Test
	void student_cannotViewAnotherStudentsAttendance() {
		Long studentUserId = createUserWithRole("student2-" + UUID.randomUUID().toString().substring(0, 8)
				+ "@school.test", "STUDENT");
		// Deliberately NOT linked to studentA — studentUserId has no student.userId association at all.
		String studentToken = authHelper.tokenForUser(tenantId, studentUserId, "student2@school.test", "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.when()
				.get("/api/v1/students/" + studentAPublicId + "/attendance")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void teacher_isForbiddenFromFeeBalance() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.when()
				.get("/api/v1/students/" + studentAPublicId + "/fee-balance")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void tenantAdmin_seesCorrectOutstandingBalanceAfterPartialPayment() {
		String feeStructurePublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"name\":\"Tuition Balance Test\",\"amount\":1000.00,\"frequency\":\"MONTHLY\","
						+ "\"planType\":\"Standard\"}")
				.when()
				.post("/api/v1/fee-structures")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		Long feeStructureId = withTenant(() -> feeStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(feeStructurePublicId), tenantId).orElseThrow().getId());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentAPublicId + "\"}")
				.when()
				.post("/api/v1/fee-structures/" + feeStructurePublicId + "/assignments")
				.then()
				.statusCode(HttpStatus.CREATED.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentAId)
						+ "\",\"feeStructurePublicId\":\"" + publicIds.of(FeeStructure.class, feeStructureId)
						+ "\",\"paidAmount\":400.00,\"paidAt\":\"2026-02-01T10:00:00\","
						+ "\"receiptNumber\":\"RCPT-BAL-1\"}")
				.when()
				.post("/api/v1/fee-payments")
				.then()
				.statusCode(HttpStatus.CREATED.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.when()
				.get("/api/v1/students/" + studentAPublicId + "/fee-balance")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.find { it.feeStructurePublicId == '" + publicIds.of(FeeStructure.class, feeStructureId)
						+ "' }.outstandingBalance",
						org.hamcrest.Matchers
								.comparesEqualTo(600.00f));
	}

	private String createStudent(String studentCode, String email) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentCode\":\"" + studentCode + "\",\"firstName\":\"Alice\",\"lastName\":\"Smith\","
						+ "\"email\":\"" + email + "\",\"dateOfBirth\":\"2010-01-01\"}")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String createGuardian(Long userId) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane-"
						+ UUID.randomUUID().toString().substring(0, 8) + "@school.test\",\"phone\":\"+14155552671\","
						+ "\"userPublicId\":\"" + publicIds.of(User.class, userId) + "\"}")
				.when()
				.post("/api/v1/guardians")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private void linkGuardianToStudent(String guardianPublicId, String studentPublicId) {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\",\"relationshipType\":\"MOTHER\","
						+ "\"primaryContact\":true}")
				.when()
				.post("/api/v1/guardians/" + guardianPublicId + "/students")
				.then()
				.statusCode(HttpStatus.CREATED.value());
	}

	private Long createUserWithRole(String email, String roleName) {
		return withTenant(() -> {
			// STUDENT/PARENT are global roles (tenant_id = NULL) — see Role.java.
			var role = roleRepository.findAll().stream()
					.filter(r -> r.getTenantId() == null && roleName.equals(r.getName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Role not seeded for tenant: " + roleName));
			User user = User.builder()
					.email(email)
					.passwordHash(passwordEncoder.encode("Password123!"))
					.status(UserStatus.ACTIVE)
					.emailVerified(true)
					.build();
			user.addRole(role);
			return userRepository.save(user).getId();
		});
	}

	private <T> T withTenant(java.util.function.Supplier<T> action) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return action.get();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String login(String email, String password) {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given()
					.header("X-Tenant-ID", tenantId)
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
