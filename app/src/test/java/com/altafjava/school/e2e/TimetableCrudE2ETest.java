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
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation.
 *
 * <p>
 * Note: {@code CreateTimetableEntryRequest} requires raw internal IDs for period/classroom/
 * subject/teacher (same pattern as {@code ScheduleExamRequest} — see the note in
 * {@code ExamCrudE2ETest}). This test resolves them via direct repository lookup.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimetableCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TestPublicIds publicIds;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private PeriodRepository periodRepository;

	@Autowired
	private ClassroomRepository classroomRepository;

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private TeacherRepository teacherRepository;

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
				"Timetable E2E School", "ttb-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private record Fixture(Long periodId, Long classroomId, Long subjectId, Long teacherId) {
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

	private Fixture createFixture(String accessToken, String suffix) {
		String academicYearPublicId = createAcademicYear(suffix + "-2025-26");
		String periodPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"name\":\"Period-" + suffix + "\",\"startTime\":\"09:00:00\",\"endTime\":\"09:45:00\","
						+ "\"displayOrder\":1}")
				.when()
				.post("/api/v1/periods")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String classroomPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"classCode\":\"CLS-" + suffix + "\",\"grade\":\"Grade 5\",\"section\":\"A\","
						+ "\"academicYearPublicId\":\"" + academicYearPublicId + "\"}")
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String subjectPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"code\":\"SUB-" + suffix + "\",\"name\":\"Mathematics\"}")
				.when()
				.post("/api/v1/subjects")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String teacherPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"employeeCode\":\"EMP-" + suffix + "\",\"firstName\":\"Jane\",\"lastName\":\"Doe\","
						+ "\"email\":\"jane-" + suffix + "@school.test\",\"joinDate\":\"2020-08-01\"}")
				.when()
				.post("/api/v1/teachers")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			Long periodId = periodRepository.findByPublicIdAndTenantId(UUID.fromString(periodPublicId), tenantId)
					.orElseThrow().getId();
			Long classroomId = classroomRepository
					.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId).orElseThrow().getId();
			Long subjectId = subjectRepository.findByPublicIdAndTenantId(UUID.fromString(subjectPublicId), tenantId)
					.orElseThrow().getId();
			Long teacherId = teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
					.orElseThrow().getId();
			return new Fixture(periodId, classroomId, subjectId, teacherId);
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String requestBody(Fixture fixture, String dayOfWeek) {
		return "{\"dayOfWeek\":\"" + dayOfWeek + "\",\"periodPublicId\":\""
				+ publicIds.of(Period.class, fixture.periodId()) + "\",\"classroomPublicId\":\""
				+ publicIds.of(Classroom.class, fixture.classroomId()) + "\",\"subjectPublicId\":\""
				+ publicIds.of(Subject.class, fixture.subjectId()) + "\",\"teacherPublicId\":\""
				+ publicIds.of(Employee.class, fixture.teacherId()) + "\"}";
	}

	@Test
	void scheduleTimetableEntry_asTenantAdmin_returns201() {
		String accessToken = login();
		Fixture fixture = createFixture(accessToken, "TT1");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(requestBody(fixture, "MONDAY"))
				.when()
				.post("/api/v1/timetable-entries")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.dayOfWeek", equalTo("MONDAY"));
	}

	@Test
	void scheduleTimetableEntry_classroomAlreadyBooked_returns400() {
		String accessToken = login();
		Fixture fixture = createFixture(accessToken, "TT2");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(requestBody(fixture, "TUESDAY"))
				.when()
				.post("/api/v1/timetable-entries")
				.then()
				.statusCode(HttpStatus.CREATED.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(requestBody(fixture, "TUESDAY"))
				.when()
				.post("/api/v1/timetable-entries")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void listTimetableEntries_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/timetable-entries")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void scheduleTimetableEntry_asTeacherRole_returns403() {
		String accessToken = login();
		Fixture fixture = createFixture(accessToken, "TT3");
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body(requestBody(fixture, "WEDNESDAY"))
				.when()
				.post("/api/v1/timetable-entries")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void timetableEntryCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		Fixture fixture = createFixture(accessToken, "TT4");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(requestBody(fixture, "THURSDAY"))
				.when()
				.post("/api/v1/timetable-entries")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "ttb-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/timetable-entries/" + publicId)
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
