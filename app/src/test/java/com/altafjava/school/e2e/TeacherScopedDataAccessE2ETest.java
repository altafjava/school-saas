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
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Proves TESTING.md's "still deferred after Phase 2" item is now real: a TEACHER only sees
 * grades/attendance from classrooms they are the assigned class teacher of, not every classroom
 * tenant-wide (which is still correct for TENANT_ADMIN, and was the TEACHER behavior before this
 * phase too).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeacherScopedDataAccessE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private ClassroomRepository classroomRepository;

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;

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
				"Teacher Scope E2E School", "tsc-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
		adminToken = login(adminEmail, adminPassword);
	}

	@Test
	void teacher_seesOnlyGradesAndAttendanceFromTheirOwnClassroom() {
		String suffix = UUID.randomUUID().toString().substring(0, 6);

		Long teacherAUserId = createTeacherUser("teacher-a-" + suffix + "@school.test");
		String teacherAPublicId = createTeacher("EMP-A-" + suffix);
		linkTeacherToUser(teacherAPublicId, teacherAUserId);
		Long teacherAId = internalTeacherId(teacherAPublicId);

		String classroomAPublicId = createClassroom("CLS-A-" + suffix, teacherAId);
		String classroomBPublicId = createClassroom("CLS-B-" + suffix, null);
		Long classroomAId = internalClassroomId(classroomAPublicId);
		Long classroomBId = internalClassroomId(classroomBPublicId);

		Long subjectId = internalSubjectId(createSubject("SUB-" + suffix));
		String studentAPublicId = createStudent("STU-A-" + suffix);
		String studentBPublicId = createStudent("STU-B-" + suffix);
		Long studentAId = internalStudentIdFromPublicId(studentAPublicId);
		Long studentBId = internalStudentIdFromPublicId(studentBPublicId);

		Long examAId = internalExamId(createExam("Exam A", subjectId, classroomAId));
		Long examBId = internalExamId(createExam("Exam B", subjectId, classroomBId));
		recordGrade(studentAId, examAId);
		recordGrade(studentBId, examBId);
		enrollStudent(classroomAPublicId, studentAPublicId);
		enrollStudent(classroomBPublicId, studentBPublicId);
		markAttendance(studentAId, classroomAId);
		markAttendance(studentBId, classroomBId);

		String teacherAToken = authHelper.tokenForUser(tenantId, teacherAUserId, "teacher-a-" + suffix + "@school.test",
				"TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherAToken)
				.when()
				.get("/api/v1/grades?size=100")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", org.hamcrest.Matchers.equalTo(1))
				.body("data.content[0].examId", org.hamcrest.Matchers.equalTo(examAId.intValue()));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherAToken)
				.when()
				.get("/api/v1/attendance?size=100")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", org.hamcrest.Matchers.equalTo(1));
	}

	private Long createTeacherUser(String email) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			// TEACHER is a global role (tenant_id = NULL) — see Role.java.
			var role = roleRepository.findAll().stream()
					.filter(r -> r.getTenantId() == null && "TEACHER".equals(r.getName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("TEACHER role not seeded"));
			User user = User.builder()
					.email(email)
					.passwordHash(passwordEncoder.encode("Password123!"))
					.status(UserStatus.ACTIVE)
					.emailVerified(true)
					.build();
			user.addRole(role);
			return userRepository.save(user).getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
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

	private void linkTeacherToUser(String teacherPublicId, Long userId) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			var teacher = teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
					.orElseThrow();
			teacher.setUserId(userId);
			teacherRepository.save(teacher);
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private Long internalTeacherId(String publicId) {
		return withTenant(() -> teacherRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow().getId());
	}

	private String createClassroom(String classCode, Long classTeacherId) {
		String teacherField = classTeacherId == null ? "" : ",\"classTeacherId\":" + classTeacherId;
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"classCode\":\"" + classCode + "\",\"grade\":\"Grade 5\",\"section\":\"A\","
						+ "\"academicYearPublicId\":\"" + createAcademicYear(classCode + "-2025-26") + "\""
						+ teacherField + "}")
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private Long internalClassroomId(String publicId) {
		return withTenant(() -> classroomRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow().getId());
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

	private Long internalSubjectId(String publicId) {
		return withTenant(() -> subjectRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow().getId());
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

	private Long internalStudentIdFromPublicId(String publicId) {
		return withTenant(() -> studentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow().getId());
	}

	private String createExam(String title, Long subjectId, Long classroomId) {
		Long examTypeId = withTenant(
				() -> examTypeDefinitionRepository.findByCodeAndTenantId("MIDTERM", tenantId).orElseThrow().getId());
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"title\":\"" + title + "\",\"subjectId\":" + subjectId + ",\"classroomId\":" + classroomId
						+ ",\"scheduledAt\":\"2026-03-01T09:00:00\",\"maxMarks\":100,\"examTypeId\":" + examTypeId
						+ "}")
				.when()
				.post("/api/v1/exams")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private Long internalExamId(String publicId) {
		return withTenant(() -> examRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow().getId());
	}

	private void recordGrade(Long studentId, Long examId) {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"examId\":" + examId + ",\"marks\":85,\"gradedBy\":"
						+ "\"admin\"}")
				.when()
				.post("/api/v1/grades")
				.then()
				.statusCode(HttpStatus.CREATED.value());
	}

	private String createAcademicYear(String name) {
		return withTenant(() -> academicYearService.create(name, java.time.LocalDate.of(2025, 6, 1),
				java.time.LocalDate.of(2026, 5, 31), true).getPublicId().toString());
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

	private void markAttendance(Long studentId, Long classroomId) {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-01\",\"status\":\"PRESENT\",\"markedBy\":\"admin\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.CREATED.value());
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
