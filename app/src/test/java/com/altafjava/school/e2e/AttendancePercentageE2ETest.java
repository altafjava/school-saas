package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import java.time.LocalDate;
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
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation. See {@code AttendanceCrudE2ETest} for the underlying setup conventions.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendancePercentageE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TestPublicIds publicIds;

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
				"Attendance Pct E2E School", "att-pct-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String createAcademicYear(String name) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return academicYearService.create(name, LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true)
					.getPublicId().toString();
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

	private void markAttendance(String accessToken, long classroomId, long studentId, LocalDate date,
			String status) {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId) + "\",\"classroomPublicId\":\""
						+ publicIds.of(Classroom.class, classroomId) + "\",\"attendanceDate\":\""
						+ date + "\",\"status\":\"" + status + "\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.CREATED.value());
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
	void attendancePercentage_asTenantAdmin_returnsComputedPercentage() {
		String accessToken = login();
		String classroomPublicId = createClassroomPublicId(accessToken, "PCT1");
		String studentPublicId = createStudentPublicId(accessToken, "STU-PCT1");
		enrollStudent(accessToken, classroomPublicId, studentPublicId);
		long classroomId = resolveClassroomId(classroomPublicId);
		long studentId = resolveStudentId(studentPublicId);
		LocalDate day1 = LocalDate.now().minusDays(2);
		LocalDate day2 = LocalDate.now().minusDays(1);
		markAttendance(accessToken, classroomId, studentId, day1, "PRESENT");
		markAttendance(accessToken, classroomId, studentId, day2, "ABSENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/students/" + studentPublicId + "/attendance/percentage?fromDate=" + day1 + "&toDate="
						+ day2)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.presentDays", org.hamcrest.Matchers.equalTo(1))
				.body("data.totalMarkedDays", org.hamcrest.Matchers.equalTo(2))
				.body("data.percentage", org.hamcrest.Matchers.comparesEqualTo(50.00f));
	}

	@Test
	void attendancePercentage_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.when()
				.get("/api/v1/students/" + UUID.randomUUID() + "/attendance/percentage?fromDate=2026-01-01&toDate="
						+ "2026-01-31")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void attendancePercentage_asUnrelatedStudentRole_returns403() {
		String accessToken = login();
		String classroomPublicId = createClassroomPublicId(accessToken, "PCT2");
		String studentPublicId = createStudentPublicId(accessToken, "STU-PCT2");
		enrollStudent(accessToken, classroomPublicId, studentPublicId);
		String studentToken = authHelper.tokenWithRole(tenantId, "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.when()
				.get("/api/v1/students/" + studentPublicId + "/attendance/percentage?fromDate=2026-01-01&toDate="
						+ "2026-01-31")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void attendancePercentage_forStudentUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String classroomPublicId = createClassroomPublicId(accessToken, "PCT3");
		String studentPublicId = createStudentPublicId(accessToken, "STU-PCT3");
		enrollStudent(accessToken, classroomPublicId, studentPublicId);

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other Pct School", "att-pct-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/students/" + studentPublicId + "/attendance/percentage?fromDate=2026-01-01&toDate="
						+ "2026-01-31")
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
