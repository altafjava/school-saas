package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import io.restassured.RestAssured;

// Scanning the QR on a student's ID card marks them present for today.
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendanceQrScanE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;

	private School school;
	private String classroomPublicId;

	@BeforeAll
	void setUp() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Scan School");
		classroomPublicId = support.createClassroom(school, "SCAN-1");
	}

	// A fresh student per test, so one test's scan never counts as another's "already marked".
	private String enrolledStudent() {
		String studentPublicId = support.createStudent(school);
		support.request(school)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\",\"academicYearPublicId\":\""
						+ support.currentAcademicYearPublicId(school) + "\"}")
				.post("/api/v1/classrooms/" + classroomPublicId + "/students")
				.then().statusCode(HttpStatus.CREATED.value());
		return studentPublicId;
	}

	private String issueCard(String studentPublicId) {
		return support.request(school).post("/api/v1/students/" + studentPublicId + "/id-card")
				.then().statusCode(HttpStatus.CREATED.value()).extract().path("data.verificationCode");
	}

	private io.restassured.response.ValidatableResponse scan(String payload) {
		return support.request(school).body("{\"payload\":\"" + payload + "\"}")
				.post("/api/v1/attendance/scan").then();
	}

	@Test
	void scan_marksPresentOnceAndThenReportsItAlreadyMarked() {
		String studentPublicId = enrolledStudent();
		String code = issueCard(studentPublicId);

		scan(code).statusCode(HttpStatus.OK.value())
				.body("data.alreadyMarked", equalTo(false))
				.body("data.status", equalTo("PRESENT"))
				.body("data.studentPublicId", equalTo(studentPublicId))
				.body("data.className", equalTo("Grade 5 A"));
		scan("https://greenfield.school.example/api/v1/documents/verify/" + code)
				.statusCode(HttpStatus.OK.value())
				.body("data.alreadyMarked", equalTo(true));

		support.request(school).get("/api/v1/students/" + studentPublicId + "/attendance")
				.then().statusCode(HttpStatus.OK.value()).body("data.content", hasSize(1));
	}

	@Test
	void scan_aReplacedCard_isRefusedWhileTheNewOneWorks() {
		String studentPublicId = enrolledStudent();
		String lostCard = issueCard(studentPublicId);
		String replacement = issueCard(studentPublicId);

		scan(lostCard).statusCode(HttpStatus.BAD_REQUEST.value())
				.body("error.message", containsString("revoked"));
		scan(replacement).statusCode(HttpStatus.OK.value());
	}

	@Test
	void scan_anUnknownCode_returns404() {
		scan("Zz9Zz9Zz").statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void scan_somethingThatIsNotACode_returns400() {
		scan("not a card").statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void scan_aCardFromAnotherSchool_returns404() {
		String code = issueCard(enrolledStudent());
		School other = support.provisionSchool("Other Scan School");

		support.request(other).body("{\"payload\":\"" + code + "\"}").post("/api/v1/attendance/scan")
				.then().statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void scan_asStudentRole_returns403() {
		support.requestWithRole(school.tenantId(), "STUDENT").body("{\"payload\":\"Zz9Zz9Zz\"}")
				.post("/api/v1/attendance/scan").then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void scan_withoutJwt_returns401() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId())
				.contentType(io.restassured.http.ContentType.JSON).body("{\"payload\":\"Zz9Zz9Zz\"}")
				.post("/api/v1/attendance/scan").then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}
}
