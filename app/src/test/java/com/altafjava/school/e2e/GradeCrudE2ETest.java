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
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
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
class GradeCrudE2ETest extends SchoolIntegrationTestBase {

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
	private ExamRepository examRepository;

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;

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
				"Grade E2E School", "grd-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
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

	private Long createExamId(String accessToken, String classCode) {
		String academicYearPublicId = createAcademicYear(classCode + "-2025-26");
		String classroomPublicId = given()
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
		Long classroomId = withTenant(() -> classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId).orElseThrow().getId());

		String subjectPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"code\":\"MATH-" + classCode + "\",\"name\":\"Math\"}")
				.when()
				.post("/api/v1/subjects")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		Long subjectId = withTenant(() -> subjectRepository
				.findByPublicIdAndTenantId(UUID.fromString(subjectPublicId), tenantId).orElseThrow().getId());

		Long examTypeId = withTenant(
				() -> examTypeDefinitionRepository.findByCodeAndTenantId("MIDTERM", tenantId).orElseThrow().getId());
		String examPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"title\":\"Midterm\",\"subjectPublicId\":\"" + publicIds.of(Subject.class, subjectId)
						+ "\",\"classroomPublicId\":\"" + publicIds.of(Classroom.class, classroomId)
						+ "\",\"scheduledAt\":\"2026-03-01T09:00:00\",\"maxMarks\":100,\"examTypePublicId\":\""
						+ publicIds.of(ExamTypeDefinition.class, examTypeId)
						+ "\"}")
				.when()
				.post("/api/v1/exams")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		return withTenant(() -> examRepository
				.findByPublicIdAndTenantId(UUID.fromString(examPublicId), tenantId).orElseThrow().getId());
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

	private Long withTenant(java.util.function.Supplier<Long> action) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return action.get();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	@Test
	void recordGrade_asTenantAdmin_returns201() {
		String accessToken = login();
		Long examId = createExamId(accessToken, "CLS-GR1");
		Long studentId = createStudentId(accessToken, "STU-GR1");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId) + "\",\"examPublicId\":\""
						+ publicIds.of(Exam.class, examId)
						+ "\",\"marks\":95}")
				.when()
				.post("/api/v1/grades")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.gradeLetter", equalTo("A"));
	}

	@Test
	void listGrades_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/grades")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void recordGrade_asStudentRole_returns403() {
		String accessToken = login();
		Long examId = createExamId(accessToken, "CLS-GR2");
		Long studentId = createStudentId(accessToken, "STU-GR2");
		String studentToken = authHelper.tokenWithRole(tenantId, "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId) + "\",\"examPublicId\":\""
						+ publicIds.of(Exam.class, examId)
						+ "\",\"marks\":90}")
				.when()
				.post("/api/v1/grades")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void gradeCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		Long examId = createExamId(accessToken, "CLS-GR3");
		Long studentId = createStudentId(accessToken, "STU-GR3");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId) + "\",\"examPublicId\":\""
						+ publicIds.of(Exam.class, examId)
						+ "\",\"marks\":75}")
				.when()
				.post("/api/v1/grades")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "grd-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/grades/" + publicId)
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
