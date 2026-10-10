package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.domain.user.model.User;
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
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;

// Grades stay invisible to a student's family until the school publishes the exam's results.
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExamResultPublicationE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TestPublicIds publicIds;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private ExamRepository examRepository;
	@Autowired
	private ClassroomRepository classroomRepository;
	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;

	private School school;
	private String studentPublicId;
	private String examPublicId;
	private String gradePublicId;
	private Long parentUserId;

	@BeforeAll
	void setUp() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Publication School");
		studentPublicId = support.createStudent(school);
		Long studentId = support.inTenant(school.tenantId(), () -> studentRepository
				.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), school.tenantId()).orElseThrow().getId());

		String classroomPublicId = support.createClassroom(school, "PUB-1");
		String subjectPublicId = support.createSubject(school, "PUB-MATH");
		Long classroomId = support.inTenant(school.tenantId(), () -> classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), school.tenantId()).orElseThrow()
				.getId());
		Long subjectId = support.inTenant(school.tenantId(), () -> subjectRepository
				.findByPublicIdAndTenantId(UUID.fromString(subjectPublicId), school.tenantId()).orElseThrow()
				.getId());
		Long examTypeId = support.inTenant(school.tenantId(), () -> examTypeDefinitionRepository
				.findByCodeAndTenantId("MIDTERM", school.tenantId()).orElseThrow().getId());

		examPublicId = support.request(school)
				.body("{\"title\":\"Midterm\",\"subjectPublicId\":\"" + publicIds.of(Subject.class, subjectId)
						+ "\",\"classroomPublicId\":\"" + publicIds.of(Classroom.class, classroomId)
						+ "\",\"scheduledAt\":\"2026-03-01T09:00:00\",\"maxMarks\":100,\"examTypePublicId\":\""
						+ publicIds.of(ExamTypeDefinition.class, examTypeId)
						+ "\",\"weightage\":40}")
				.post("/api/v1/exams")
				.then().statusCode(HttpStatus.CREATED.value())
				.body("data.weightage", equalTo(40))
				.body("data.resultsPublished", equalTo(false))
				.extract().path("data.publicId");
		Long examId = support.inTenant(school.tenantId(), () -> examRepository
				.findByPublicIdAndTenantId(UUID.fromString(examPublicId), school.tenantId()).orElseThrow().getId());

		gradePublicId = support.request(school)
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId) + "\",\"examPublicId\":\""
						+ publicIds.of(Exam.class, examId) + "\",\"marks\":85}")
				.post("/api/v1/grades")
				.then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		parentUserId = support.createUserWithRole(school.tenantId(), "parent-" + UUID.randomUUID() + "@school.test",
				"PARENT");
		String guardianPublicId = support.request(school)
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane-" + UUID.randomUUID()
						+ "@school.test\",\"phone\":\"+14155552671\",\"userPublicId\":\""
						+ publicIds.of(User.class, parentUserId) + "\"}")
				.post("/api/v1/guardians")
				.then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		support.request(school)
				.body("{\"studentPublicId\":\"" + studentPublicId
						+ "\",\"relationshipType\":\"MOTHER\",\"primaryContact\":true}")
				.post("/api/v1/guardians/" + guardianPublicId + "/students")
				.then().statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void resultsFlow_hidesGradesFromTheFamilyUntilPublishedAndAgainAfterWithdrawal() {
		support.requestAsUser(school.tenantId(), parentUserId, "PARENT")
				.get("/api/v1/students/" + studentPublicId + "/grades")
				.then().statusCode(HttpStatus.OK.value()).body("data.content", hasSize(0));
		support.requestAsUser(school.tenantId(), parentUserId, "PARENT")
				.get("/api/v1/grades/" + gradePublicId)
				.then().statusCode(HttpStatus.NOT_FOUND.value());
		support.request(school).get("/api/v1/students/" + studentPublicId + "/grades")
				.then().statusCode(HttpStatus.OK.value()).body("data.content", hasSize(1));

		support.request(school).post("/api/v1/exams/" + examPublicId + "/results/publish")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
		support.request(school).patch("/api/v1/exams/" + examPublicId + "/complete")
				.then().statusCode(HttpStatus.OK.value());
		support.request(school).post("/api/v1/exams/" + examPublicId + "/results/publish")
				.then().statusCode(HttpStatus.OK.value())
				.body("data.resultsPublished", equalTo(true))
				.body("data.resultsPublishedAt", notNullValue());

		support.requestAsUser(school.tenantId(), parentUserId, "PARENT")
				.get("/api/v1/students/" + studentPublicId + "/grades")
				.then().statusCode(HttpStatus.OK.value()).body("data.content", hasSize(1));
		support.requestAsUser(school.tenantId(), parentUserId, "PARENT")
				.get("/api/v1/grades/" + gradePublicId)
				.then().statusCode(HttpStatus.OK.value());

		support.request(school).post("/api/v1/exams/" + examPublicId + "/results/withdraw")
				.then().statusCode(HttpStatus.OK.value()).body("data.resultsPublished", equalTo(false));
		support.requestAsUser(school.tenantId(), parentUserId, "PARENT")
				.get("/api/v1/students/" + studentPublicId + "/grades")
				.then().statusCode(HttpStatus.OK.value()).body("data.content", hasSize(0));
	}

	@Test
	void reweight_beforePublication_updatesTheWeightage() {
		support.request(school).body("{\"weightage\":60}")
				.patch("/api/v1/exams/" + examPublicId + "/weightage")
				.then().statusCode(HttpStatus.OK.value()).body("data.weightage", equalTo(60));
		support.request(school).body("{\"weightage\":0}")
				.patch("/api/v1/exams/" + examPublicId + "/weightage")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void publishResults_asTeacher_returns403() {
		support.requestWithRole(school.tenantId(), "TEACHER")
				.post("/api/v1/exams/" + examPublicId + "/results/publish")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void publishResults_withoutJwt_returns401() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId())
				.post("/api/v1/exams/" + examPublicId + "/results/publish")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void corrections_asParent_returns403() {
		support.requestAsUser(school.tenantId(), parentUserId, "PARENT")
				.get("/api/v1/grades/" + gradePublicId + "/corrections")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}
}
