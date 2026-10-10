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
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation.
 *
 * <p>
 * Note: {@code ScheduleExamRequest.classroomId} requires the classroom's raw internal ID, but
 * {@code ClassroomResponse} (correctly, per CLAUDE.md's DTO rule) exposes only {@code publicId} —
 * there is currently no HTTP-only way for a real client to obtain a valid {@code classroomId}.
 * This test resolves it via direct repository lookup, standing in for what should eventually be a
 * request-side fix (accept the classroom's public UUID and resolve it server-side).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExamCrudE2ETest extends SchoolIntegrationTestBase {

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
				"Exam E2E School", "exam-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String createAcademicYear(String name) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null,
				com.altafjava.platform.core.tenant.TenantType.SHARED);
		try {
			return academicYearService.create(name, java.time.LocalDate.of(2025, 6, 1),
					java.time.LocalDate.of(2026, 5, 31), true).getPublicId().toString();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private Long createClassroomAndGetInternalId(String accessToken, String classCode) {
		String academicYearPublicId = createAcademicYear(classCode + "-2025-26");
		String publicId = given()
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

		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null,
				com.altafjava.platform.core.tenant.TenantType.SHARED);
		try {
			return classroomRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
					.orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	// SchoolTenantProvisioningListener seeds UNIT_TEST/MIDTERM/FINAL/QUIZ for every new tenant.
	private Long examTypeIdFor(String code) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null,
				com.altafjava.platform.core.tenant.TenantType.SHARED);
		try {
			return examTypeDefinitionRepository.findByCodeAndTenantId(code, tenantId).orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private Long createSubjectAndGetInternalId(String accessToken, String code) {
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"code\":\"" + code + "\",\"name\":\"" + code + "\"}")
				.when()
				.post("/api/v1/subjects")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null,
				com.altafjava.platform.core.tenant.TenantType.SHARED);
		try {
			return subjectRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
					.orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	@Test
	void scheduleExam_asTenantAdmin_returns201() {
		String accessToken = login();
		Long classroomId = createClassroomAndGetInternalId(accessToken, "CLS-EX1");
		Long subjectId = createSubjectAndGetInternalId(accessToken, "MATH-EX1");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"title\":\"Midterm\",\"subjectPublicId\":\"" + publicIds.of(Subject.class, subjectId)
						+ "\",\"classroomPublicId\":\"" + publicIds.of(Classroom.class, classroomId)
						+ "\",\"scheduledAt\":\"2026-03-01T09:00:00\",\"maxMarks\":100,\"examTypePublicId\":\""
						+ publicIds.of(ExamTypeDefinition.class, examTypeIdFor("MIDTERM")) + "\"}")
				.when()
				.post("/api/v1/exams")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.title", equalTo("Midterm"));
	}

	@Test
	void listExams_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/exams")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void scheduleExam_asStudentRole_returns403() {
		String accessToken = login();
		Long classroomId = createClassroomAndGetInternalId(accessToken, "CLS-EX2");
		Long subjectId = createSubjectAndGetInternalId(accessToken, "SCI-EX2");
		String studentToken = authHelper.tokenWithRole(tenantId, "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.contentType(ContentType.JSON)
				.body("{\"title\":\"Final\",\"subjectPublicId\":\"" + publicIds.of(Subject.class, subjectId)
						+ "\",\"classroomPublicId\":\"" + publicIds.of(Classroom.class, classroomId)
						+ "\",\"scheduledAt\":\"2026-05-01T09:00:00\",\"maxMarks\":100,\"examTypePublicId\":\""
						+ publicIds.of(ExamTypeDefinition.class, examTypeIdFor("FINAL")) + "\"}")
				.when()
				.post("/api/v1/exams")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void examCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		Long classroomId = createClassroomAndGetInternalId(accessToken, "CLS-EX3");
		Long subjectId = createSubjectAndGetInternalId(accessToken, "HIST-EX3");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"title\":\"Quiz\",\"subjectPublicId\":\"" + publicIds.of(Subject.class, subjectId)
						+ "\",\"classroomPublicId\":\"" + publicIds.of(Classroom.class, classroomId)
						+ "\",\"scheduledAt\":\"2026-04-01T09:00:00\",\"maxMarks\":50,\"examTypePublicId\":\""
						+ publicIds.of(ExamTypeDefinition.class, examTypeIdFor("QUIZ")) + "\"}")
				.when()
				.post("/api/v1/exams")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "exam-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/exams/" + publicId)
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void completeThenCancelExam_asTenantAdmin_transitionsStatus() {
		String accessToken = login();
		Long classroomId = createClassroomAndGetInternalId(accessToken, "CLS-EX4");
		Long subjectId = createSubjectAndGetInternalId(accessToken, "MATH-EX4");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"title\":\"Unit Test\",\"subjectPublicId\":\"" + publicIds.of(Subject.class, subjectId)
						+ "\",\"classroomPublicId\":\"" + publicIds.of(Classroom.class, classroomId)
						+ "\",\"scheduledAt\":\"2026-03-05T09:00:00\",\"maxMarks\":50,\"examTypePublicId\":\""
						+ publicIds.of(ExamTypeDefinition.class, examTypeIdFor("UNIT_TEST")) + "\"}")
				.when()
				.post("/api/v1/exams")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("SCHEDULED"))
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.when()
				.patch("/api/v1/exams/" + publicId + "/complete")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("COMPLETED"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.when()
				.patch("/api/v1/exams/" + publicId + "/cancel")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void cancelExam_asTeacherRole_returns403() {
		String accessToken = login();
		Long classroomId = createClassroomAndGetInternalId(accessToken, "CLS-EX5");
		Long subjectId = createSubjectAndGetInternalId(accessToken, "SCI-EX5");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"title\":\"Quiz\",\"subjectPublicId\":\"" + publicIds.of(Subject.class, subjectId)
						+ "\",\"classroomPublicId\":\"" + publicIds.of(Classroom.class, classroomId)
						+ "\",\"scheduledAt\":\"2026-03-06T09:00:00\",\"maxMarks\":50,\"examTypePublicId\":\""
						+ publicIds.of(ExamTypeDefinition.class, examTypeIdFor("QUIZ")) + "\"}")
				.when()
				.post("/api/v1/exams")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.when()
				.patch("/api/v1/exams/" + publicId + "/cancel")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
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
