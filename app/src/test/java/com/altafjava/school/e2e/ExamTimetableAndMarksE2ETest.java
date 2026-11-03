package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * The exam timetable and the marks stages over one school: two classrooms of the current year
 * sharing a hall, a parent of a student in the first, and a teacher who enters marks.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExamTimetableAndMarksE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private TestPublicIds publicIds;
	@Autowired
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;

	private School school;
	private String classroomA;
	private String classroomB;
	private String subject;
	private String examType;
	private String hall;
	private String student;
	private Long parentUser;
	private int examsScheduled;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Exam Timetable School");
		String year = support.currentAcademicYearPublicId(school);
		classroomA = classroom("TT-A", year);
		classroomB = classroom("TT-B", year);
		subject = support.createSubject(school, "TT-MATH");
		examType = support.inTenant(school.tenantId(), () -> publicIds.of(ExamTypeDefinition.class,
				examTypeDefinitionRepository.findByCodeAndTenantId("MIDTERM", school.tenantId()).orElseThrow()
						.getId()));
		hall = admin().body("{\"code\":\"HALL-" + suffix() + "\",\"name\":\"Main hall\",\"venueType\":\"HALL\"}")
				.post("/api/v1/venues").then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		student = support.createStudent(school);
		admin().body("{\"studentPublicId\":\"" + student + "\",\"academicYearPublicId\":\"" + year + "\"}")
				.post("/api/v1/classrooms/" + classroomA + "/students").then().statusCode(HttpStatus.CREATED.value());
		parentUser = support.createUserWithRole(school.tenantId(), "parent-" + suffix() + "@school.test", "PARENT");
		String guardian = admin().body("{\"firstName\":\"Pat\",\"lastName\":\"Doe\",\"email\":\"pat-" + suffix()
				+ "@school.test\",\"phone\":\"+14155552671\",\"userPublicId\":\"" + publicIds.of(User.class, parentUser)
				+ "\"}").post("/api/v1/guardians").then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		admin().body(
				"{\"studentPublicId\":\"" + student + "\",\"relationshipType\":\"MOTHER\",\"primaryContact\":true}")
				.post("/api/v1/guardians/" + guardian + "/students").then().statusCode(HttpStatus.CREATED.value());
	}

	// ---- timetable ----

	@Test
	void schedule_overlappingAnotherExamOfTheClassroom_isRefused() {
		String day = nextDay();
		schedule(classroomA, day + "T09:00:00", 120, null).then().statusCode(HttpStatus.CREATED.value());

		schedule(classroomA, day + "T10:00:00", 60, null).then().statusCode(HttpStatus.BAD_REQUEST.value())
				.body("error.message", containsString("same classroom"));
		schedule(classroomA, day + "T11:00:00", 60, null).then().statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void schedule_overlappingAnotherClassroomInTheSameVenue_isRefused() {
		String day = nextDay();
		schedule(classroomA, day + "T09:00:00", 90, hall).then().statusCode(HttpStatus.CREATED.value())
				.body("data.venue.name", equalTo("Main hall"))
				.body("data.endsAt", equalTo(day + "T10:30:00"));

		schedule(classroomB, day + "T10:00:00", 60, hall).then().statusCode(HttpStatus.BAD_REQUEST.value())
				.body("error.message", containsString("same venue"));
		schedule(classroomB, day + "T10:00:00", 60, null).then().statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void reschedule_intoAClash_isRefused() {
		String day = nextDay();
		schedule(classroomA, day + "T09:00:00", 60, null).then().statusCode(HttpStatus.CREATED.value());
		String later = schedule(classroomA, day + "T12:00:00", 60, null).then().extract().path("data.publicId");

		admin().body("{\"scheduledAt\":\"" + day + "T09:30:00\",\"durationMinutes\":60,\"version\":0}")
				.patch("/api/v1/exams/" + later + "/schedule").then().statusCode(HttpStatus.BAD_REQUEST.value());
		admin().body("{\"scheduledAt\":\"" + day + "T14:00:00\",\"durationMinutes\":45,\"version\":0}")
				.patch("/api/v1/exams/" + later + "/schedule").then().statusCode(HttpStatus.OK.value())
				.body("data.durationMinutes", equalTo(45));
	}

	@Test
	void timetable_isHiddenFromFamiliesUntilPublished() {
		String exam = schedule(classroomA, nextDay() + "T09:00:00", 60, null).then().extract().path("data.publicId");

		parent().get("/api/v1/classrooms/" + classroomA + "/exam-timetable").then()
				.statusCode(HttpStatus.OK.value()).body("data.publicId", not(hasItem(exam)));
		parent().get("/api/v1/exams/" + exam).then().statusCode(HttpStatus.NOT_FOUND.value());
		admin().get("/api/v1/classrooms/" + classroomA + "/exam-timetable").then()
				.body("data.publicId", hasItem(exam));

		admin().post("/api/v1/classrooms/" + classroomA + "/exam-timetable/publish").then()
				.statusCode(HttpStatus.OK.value()).body("data.publicId", hasItem(exam))
				.body("data.schedulePublished", not(hasItem(false)));

		parent().get("/api/v1/classrooms/" + classroomA + "/exam-timetable").then()
				.body("data.publicId", hasItem(exam));
		parent().get("/api/v1/exams/" + exam).then().statusCode(HttpStatus.OK.value())
				.body("data.schedulePublished", equalTo(true));
		admin().post("/api/v1/classrooms/" + classroomA + "/exam-timetable/publish").then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void timetable_ofAClassroomTheParentHasNoChildIn_returns403() {
		parent().get("/api/v1/classrooms/" + classroomB + "/exam-timetable").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void publishTimetable_asAParent_returns403() {
		parent().post("/api/v1/classrooms/" + classroomA + "/exam-timetable/publish").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void timetable_withoutLogin_returns401() {
		given().header("X-Tenant-ID", school.tenantId()).get("/api/v1/classrooms/" + classroomA + "/exam-timetable")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void timetable_ofAnotherSchoolsClassroom_returns404() {
		School other = support.provisionSchool("Other Timetable School");

		support.request(other).get("/api/v1/classrooms/" + classroomA + "/exam-timetable").then()
				.statusCode(HttpStatus.NOT_FOUND.value());
		support.request(other).post("/api/v1/classrooms/" + classroomA + "/exam-timetable/publish").then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	// ---- marks verification ----

	@Test
	void marks_enteredThenVerifiedThenLocked_beforeResultsArePublished() {
		String exam = completedExamWithAMark();

		admin().post("/api/v1/exams/" + exam + "/results/publish").then().statusCode(HttpStatus.BAD_REQUEST.value());
		verifier().post("/api/v1/exams/" + exam + "/marks/verify").then().statusCode(HttpStatus.OK.value())
				.body("data.marksStage", equalTo("VERIFIED"));
		markEntry(exam, 70).then().statusCode(HttpStatus.BAD_REQUEST.value());
		admin().post("/api/v1/exams/" + exam + "/results/publish").then().statusCode(HttpStatus.BAD_REQUEST.value());
		admin().post("/api/v1/exams/" + exam + "/marks/lock").then().statusCode(HttpStatus.OK.value())
				.body("data.marksStage", equalTo("LOCKED"));
		admin().post("/api/v1/exams/" + exam + "/results/publish").then().statusCode(HttpStatus.OK.value());

		admin().post("/api/v1/exams/" + exam + "/marks/unlock").then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void verify_bySomeoneWhoEnteredMarks_returns403() {
		String exam = completedExamWithAMark();

		admin().post("/api/v1/exams/" + exam + "/marks/verify").then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void returnedMarks_canBeCorrectedAgain() {
		String exam = completedExamWithAMark();
		verifier().post("/api/v1/exams/" + exam + "/marks/verify").then().statusCode(HttpStatus.OK.value());

		verifier().post("/api/v1/exams/" + exam + "/marks/return").then().statusCode(HttpStatus.OK.value())
				.body("data.marksStage", equalTo("ENTERED"));
	}

	@Test
	void verify_asATeacher_returns403() {
		String exam = completedExamWithAMark();

		support.requestWithRole(school.tenantId(), "TEACHER").post("/api/v1/exams/" + exam + "/marks/verify").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void verify_withoutLogin_returns401() {
		given().header("X-Tenant-ID", school.tenantId()).post("/api/v1/exams/" + UUID.randomUUID() + "/marks/verify")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void verify_anotherSchoolsExam_returns404() {
		String exam = completedExamWithAMark();
		School other = support.provisionSchool("Other Marks School");

		support.request(other).post("/api/v1/exams/" + exam + "/marks/lock").then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	private String completedExamWithAMark() {
		String exam = schedule(classroomA, nextDay() + "T09:00:00", 60, null).then()
				.statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		markEntry(exam, 80).then().statusCode(HttpStatus.CREATED.value());
		admin().patch("/api/v1/exams/" + exam + "/complete").then().statusCode(HttpStatus.OK.value());
		return exam;
	}

	private io.restassured.response.Response markEntry(String exam, int marks) {
		return admin().body("{\"studentPublicId\":\"" + student + "\",\"examPublicId\":\"" + exam + "\",\"marks\":"
				+ marks + "}").post("/api/v1/grades");
	}

	private io.restassured.response.Response schedule(String classroom, String startsAt, int minutes, String venue) {
		return admin().body("{\"title\":\"Exam " + suffix() + "\",\"subjectPublicId\":\"" + subject
				+ "\",\"classroomPublicId\":\"" + classroom + "\",\"scheduledAt\":\"" + startsAt
				+ "\",\"durationMinutes\":" + minutes + (venue == null ? "" : ",\"venuePublicId\":\"" + venue + "\"")
				+ ",\"maxMarks\":100,\"examTypePublicId\":\"" + examType + "\",\"weightage\":1}").post("/api/v1/exams");
	}

	// Every test works on its own day, so the tests never clash with one another.
	private synchronized String nextDay() {
		return java.time.LocalDate.now().plusDays(30 + examsScheduled++).toString();
	}

	private String classroom(String code, String year) {
		return admin().body("{\"classCode\":\"" + code + "-" + suffix() + "\",\"grade\":\"5\",\"section\":\"" + code
				+ "\",\"academicYearPublicId\":\"" + year + "\"}").post("/api/v1/classrooms").then()
				.statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
	}

	private RequestSpecification admin() {
		return support.request(school);
	}

	private RequestSpecification verifier() {
		return support.requestWithRole(school.tenantId(), "ACADEMIC");
	}

	private RequestSpecification parent() {
		return support.requestAsUser(school.tenantId(), parentUser, "PARENT");
	}

	private static String suffix() {
		return UUID.randomUUID().toString().substring(0, 6);
	}
}
