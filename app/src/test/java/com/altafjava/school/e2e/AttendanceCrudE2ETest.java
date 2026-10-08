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
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation. See {@code ExamCrudE2ETest}'s class javadoc for why prerequisite IDs are
 * resolved via repository lookup rather than purely through HTTP.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendanceCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private ClassroomRepository classroomRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private AcademicYearService academicYearService;

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
				"Attendance E2E School", "att-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String createAcademicYear(String name) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return academicYearService.create(name, java.time.LocalDate.of(2025, 6, 1),
					java.time.LocalDate.of(2026, 5, 31), true).getPublicId().toString();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String createClassroomPublicId(String accessToken, String classCode) {
		String academicYearPublicId = createAcademicYear(classCode + "-2025-26");
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"classCode\":\"" + classCode + "\",\"grade\":\"Grade 5\",\"section\":\"A\","
						+ "\"academicYearPublicId\":\"" + academicYearPublicId + "\"}")
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private void enrollStudent(String accessToken, String classroomPublicId, String studentPublicId) {
		String academicYearPublicId = createAcademicYear("enroll-" + UUID.randomUUID().toString().substring(0, 8));
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\",\"academicYearPublicId\":\""
						+ academicYearPublicId + "\"}")
				.when()
				.post("/api/v1/classrooms/" + classroomPublicId + "/students")
				.then()
				.statusCode(HttpStatus.CREATED.value());
	}

	private String createStudentPublicId(String accessToken, String studentCode) {
		return given()
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
	}

	private long[] createEnrolledClassroomAndStudent(String accessToken, String suffix) {
		String classroomPublicId = createClassroomPublicId(accessToken, "CLS-" + suffix);
		String studentPublicId = createStudentPublicId(accessToken, "STU-" + suffix);
		enrollStudent(accessToken, classroomPublicId, studentPublicId);
		return new long[] { resolveClassroomId(classroomPublicId), resolveStudentId(studentPublicId) };
	}

	private Long resolveClassroomId(String publicId) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return classroomRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
					.orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private Long resolveStudentId(String publicId) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return studentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
					.orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	@Test
	void markAttendance_asTenantAdmin_returns201() {
		String accessToken = login();
		long[] ids = createEnrolledClassroomAndStudent(accessToken, "AT1");
		long classroomId = ids[0];
		long studentId = ids[1];

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-01\",\"status\":\"PRESENT\",\"markedBy\":\"admin\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.status", equalTo("PRESENT"));
	}

	@Test
	void markAttendance_withInvalidStatus_returns400NotServerError() {
		String accessToken = login();
		long[] ids = createEnrolledClassroomAndStudent(accessToken, "AT2");
		long classroomId = ids[0];
		long studentId = ids[1];

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-01\",\"status\":\"NOT_A_REAL_STATUS\",\"markedBy\":\"admin\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void listAttendance_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void markAttendance_asStudentRole_returns403() {
		String accessToken = login();
		long[] ids = createEnrolledClassroomAndStudent(accessToken, "AT3");
		long classroomId = ids[0];
		long studentId = ids[1];
		String studentToken = authHelper.tokenWithRole(tenantId, "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-02\",\"status\":\"PRESENT\",\"markedBy\":\"self\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void attendanceCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		long[] ids = createEnrolledClassroomAndStudent(accessToken, "AT4");
		long classroomId = ids[0];
		long studentId = ids[1];
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-03\",\"status\":\"ABSENT\",\"markedBy\":\"admin\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "att-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/attendance/" + publicId)
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
