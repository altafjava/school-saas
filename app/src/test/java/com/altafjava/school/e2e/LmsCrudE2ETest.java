package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation — for LessonController, AssignmentController and SubmissionController.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LmsCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TestPublicIds publicIds;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private TeacherRepository teacherRepository;

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
		String adminPassword = "Password123!";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"LMS E2E School", "lms-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
		adminToken = login(tenantId, adminEmail, adminPassword);
	}

	@Test
	void fullLmsFlow_teacherPostsContent_studentSubmits_teacherGrades() {
		String suffix = UUID.randomUUID().toString().substring(0, 6);

		Long teacherUserId = createUserWithRole("teacher-" + suffix + "@school.test", "TEACHER");
		String teacherPublicId = createTeacher("EMP-" + suffix);
		Long teacherId = linkTeacherToUser(teacherPublicId, teacherUserId);
		String classroomPublicId = createClassroom("CLS-" + suffix, teacherId);
		String subjectPublicId = createSubject("SUB-" + suffix);
		String teacherToken = authHelper.tokenForUser(tenantId, teacherUserId, "teacher-" + suffix + "@school.test",
				"TEACHER");

		String lessonPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"classroomPublicId\":\"" + classroomPublicId + "\",\"subjectPublicId\":\""
						+ subjectPublicId + "\",\"title\":\"Intro to Cells\",\"description\":\"Chapter 1\"}")
				.when()
				.post("/api/v1/lessons")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.title", equalTo("Intro to Cells"))
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.when()
				.get("/api/v1/lessons/classroom/" + classroomPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", org.hamcrest.Matchers.equalTo(1))
				.body("data.content[0].publicId", equalTo(lessonPublicId));

		String assignmentPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"classroomPublicId\":\"" + classroomPublicId + "\",\"subjectPublicId\":\""
						+ subjectPublicId + "\",\"title\":\"Essay on Rivers\",\"dueDate\":\""
						+ LocalDate.now().plusDays(7) + "\",\"maxMarks\":100}")
				.when()
				.post("/api/v1/assignments")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.when()
				.get("/api/v1/assignments/classroom/" + classroomPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", org.hamcrest.Matchers.equalTo(1));

		LocalDate newDueDate = LocalDate.now().plusDays(14);
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"dueDate\":\"" + newDueDate + "\"}")
				.when()
				.patch("/api/v1/assignments/" + assignmentPublicId + "/reschedule")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.dueDate", equalTo(newDueDate.toString()));

		Long studentUserId = createUserWithRole("student-" + suffix + "@school.test", "STUDENT");
		String studentPublicId = createStudent("STU-" + suffix);
		linkStudentToUser(studentPublicId, studentUserId);
		enrollStudent(classroomPublicId, studentPublicId);
		String studentToken = authHelper.tokenForUser(tenantId, studentUserId, "student-" + suffix + "@school.test",
				"STUDENT");

		String submissionPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.contentType(ContentType.JSON)
				.body("{\"textContent\":\"My essay content\"}")
				.when()
				.post("/api/v1/assignments/" + assignmentPublicId + "/submissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("SUBMITTED"))
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.when()
				.get("/api/v1/assignments/" + assignmentPublicId + "/submissions")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", org.hamcrest.Matchers.equalTo(1))
				.body("data.content[0].publicId", equalTo(submissionPublicId));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"marks\":88.0,\"feedback\":\"Well done\"}")
				.when()
				.patch("/api/v1/assignments/" + assignmentPublicId + "/submissions/" + submissionPublicId + "/grade")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("GRADED"))
				.body("data.marksObtained", equalTo(88.0f))
				.body("data.feedback", equalTo("Well done"));
	}

	@Test
	void postLesson_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("{\"classroomPublicId\":\"" + UUID.randomUUID() + "\",\"subjectPublicId\":\""
						+ UUID.randomUUID() + "\",\"title\":\"X\"}")
				.when()
				.post("/api/v1/lessons")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void postLesson_asStudentRole_returns403() {
		String studentToken = authHelper.tokenWithRole(tenantId, "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.contentType(ContentType.JSON)
				.body("{\"classroomPublicId\":\"" + UUID.randomUUID() + "\",\"subjectPublicId\":\""
						+ UUID.randomUUID() + "\",\"title\":\"X\"}")
				.when()
				.post("/api/v1/lessons")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void createAssignment_asParentRole_returns403() {
		String parentToken = authHelper.tokenWithRole(tenantId, "PARENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + parentToken)
				.contentType(ContentType.JSON)
				.body("{\"classroomPublicId\":\"" + UUID.randomUUID() + "\",\"subjectPublicId\":\""
						+ UUID.randomUUID() + "\",\"title\":\"X\",\"dueDate\":\"" + LocalDate.now().plusDays(1)
						+ "\"}")
				.when()
				.post("/api/v1/assignments")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void submitAssignment_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"textContent\":\"answer\"}")
				.when()
				.post("/api/v1/assignments/" + UUID.randomUUID() + "/submissions")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void gradeSubmission_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("{\"marks\":50}")
				.when()
				.patch("/api/v1/assignments/" + UUID.randomUUID() + "/submissions/" + UUID.randomUUID() + "/grade")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void assignmentCreatedUnderOneTenant_rescheduleReturns404ForAnotherTenant() {
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		Long teacherUserId = createUserWithRole("teacher2-" + suffix + "@school.test", "TEACHER");
		String teacherPublicId = createTeacher("EMP2-" + suffix);
		Long teacherId = linkTeacherToUser(teacherPublicId, teacherUserId);
		String classroomPublicId = createClassroom("CLS2-" + suffix, teacherId);
		String subjectPublicId = createSubject("SUB2-" + suffix);
		String teacherToken = authHelper.tokenForUser(tenantId, teacherUserId, "teacher2-" + suffix + "@school.test",
				"TEACHER");
		String assignmentPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"classroomPublicId\":\"" + classroomPublicId + "\",\"subjectPublicId\":\""
						+ subjectPublicId + "\",\"title\":\"Essay\",\"dueDate\":\"" + LocalDate.now().plusDays(5)
						+ "\"}")
				.when()
				.post("/api/v1/assignments")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other LMS School", "lms-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherAdminToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		// A TEACHER token is required here, not the tenant admin's — the reschedule endpoint is
		// TEACHER-only, so @PreAuthorize would 403 an admin token before the service layer's own
		// tenant-scoped lookup (the thing this test actually verifies) ever runs.
		String otherTeacherEmail = "other-teacher-" + otherSuffix + "@school.test";
		String otherTeacherPublicId = given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherAdminToken)
				.contentType(ContentType.JSON)
				.body("{\"employeeCode\":\"EMP-OTHER-" + otherSuffix + "\",\"firstName\":\"Sam\",\"lastName\":"
						+ "\"Lee\",\"email\":\"" + otherTeacherEmail + "\",\"joinDate\":\"2020-08-01\"}")
				.when()
				.post("/api/v1/teachers")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		Long otherTeacherUserId = withTenantId(otherTenant.getId(), () -> {
			var role = roleRepository.findAll().stream()
					.filter(r -> r.getTenantId() == null && "TEACHER".equals(r.getName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Role not seeded: TEACHER"));
			User user = User.builder()
					.email(otherTeacherEmail)
					.passwordHash(passwordEncoder.encode("Password123!"))
					.status(UserStatus.ACTIVE)
					.emailVerified(true)
					.build();
			user.addRole(role);
			Long userId = userRepository.save(user).getId();
			var teacher = teacherRepository
					.findByPublicIdAndTenantId(UUID.fromString(otherTeacherPublicId), otherTenant.getId())
					.orElseThrow();
			teacher.setUserId(userId);
			teacherRepository.save(teacher);
			return userId;
		});
		String otherTeacherToken = authHelper.tokenForUser(otherTenant.getId(), otherTeacherUserId, otherTeacherEmail,
				"TEACHER");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherTeacherToken)
				.contentType(ContentType.JSON)
				.body("{\"dueDate\":\"" + LocalDate.now().plusDays(20) + "\"}")
				.when()
				.patch("/api/v1/assignments/" + assignmentPublicId + "/reschedule")
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	private Long createUserWithRole(String email, String roleName) {
		return withTenant(() -> {
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

	private String createTeacher(String employeeCode) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"employeeCode\":\"" + employeeCode + "\",\"firstName\":\"Jane\",\"lastName\":\"Doe\","
						+ "\"email\":\"" + employeeCode + "@school.test\",\"joinDate\":\"2020-08-01\"}")
				.when()
				.post("/api/v1/teachers")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private Long linkTeacherToUser(String teacherPublicId, Long userId) {
		return withTenant(() -> {
			var teacher = teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
					.orElseThrow();
			teacher.setUserId(userId);
			teacherRepository.save(teacher);
			return teacher.getId();
		});
	}

	private void linkStudentToUser(String studentPublicId, Long userId) {
		withTenant(() -> {
			var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
					.orElseThrow();
			student.setUserId(userId);
			studentRepository.save(student);
			return null;
		});
	}

	private String createClassroom(String classCode, Long classTeacherId) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"classCode\":\"" + classCode + "\",\"grade\":\"Grade 5\",\"section\":\"A\","
						+ "\"academicYearPublicId\":\"" + createAcademicYear(classCode + "-2025-26") + "\""
						+ ",\"classTeacherPublicId\":\"" + publicIds.of(Employee.class, classTeacherId) + "\"}")
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String createAcademicYear(String name) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"name\":\"" + name + "\",\"startDate\":\"2025-06-01\",\"endDate\":\"2026-05-31\","
						+ "\"current\":true}")
				.when()
				.post("/api/v1/academic-years")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String createSubject(String code) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"code\":\"" + code + "\",\"name\":\"" + code + "\"}")
				.when()
				.post("/api/v1/subjects")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String createStudent(String studentCode) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentCode\":\"" + studentCode + "\",\"firstName\":\"Alice\",\"lastName\":\"Smith\","
						+ "\"email\":\"" + studentCode + "@school.test\",\"dateOfBirth\":\"2010-01-01\"}")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private void enrollStudent(String classroomPublicId, String studentPublicId) {
		String academicYearPublicId = createAcademicYear("enroll-" + UUID.randomUUID().toString().substring(0, 8));
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\",\"academicYearPublicId\":\""
						+ academicYearPublicId + "\"}")
				.when()
				.post("/api/v1/classrooms/" + classroomPublicId + "/students")
				.then()
				.statusCode(HttpStatus.CREATED.value());
	}

	private <T> T withTenant(java.util.function.Supplier<T> action) {
		return withTenantId(tenantId, action);
	}

	private <T> T withTenantId(Long forTenantId, java.util.function.Supplier<T> action) {
		TenantContext.ForTesting.setCurrentTenant(forTenantId, null, null, TenantType.SHARED);
		try {
			return action.get();
		} finally {
			TenantContext.ForTesting.clear();
		}
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
