package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.util.UUID;
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
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import io.restassured.RestAssured;

/**
 * Two admins open the same record; the second to save must be told, not silently win. Each test
 * edits with the version it read, then replays an edit with the version that is now stale.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OptimisticConcurrencyE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;

	private School school;

	@BeforeAll
	void provision() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Concurrency School");
	}

	@Test
	void student_staleEdit_isRejectedAndLeavesTheRecordUnchanged() {
		String student = support.createStudent(school);
		int readVersion = support.request(school).get("/api/v1/students/" + student).then()
				.statusCode(HttpStatus.OK.value()).body("data.version", notNullValue()).extract().path("data.version");

		support.request(school).body("{\"phone\":\"+14155550101\",\"version\":" + readVersion + "}")
				.patch("/api/v1/students/" + student + "/phone").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.phone", equalTo("+14155550101"))
				.body("data.version", equalTo(readVersion + 1));

		support.request(school).body("{\"phone\":\"+14155550999\",\"version\":" + readVersion + "}")
				.patch("/api/v1/students/" + student + "/phone").then()
				.statusCode(HttpStatus.CONFLICT.value())
				.body("error.code", equalTo("VERSION_CONFLICT"));

		support.request(school).get("/api/v1/students/" + student).then()
				.body("data.phone", equalTo("+14155550101"))
				.body("data.version", equalTo(readVersion + 1));
	}

	@Test
	void guardian_staleEdit_isRejected() {
		String guardian = support.request(school)
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane-"
						+ UUID.randomUUID().toString().substring(0, 8)
						+ "@school.test\",\"phone\":\"+14155552671\"}")
				.post("/api/v1/guardians").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		int readVersion = support.request(school).get("/api/v1/guardians/" + guardian).then()
				.statusCode(HttpStatus.OK.value()).extract().path("data.version");

		support.request(school).body("{\"phone\":\"+14155550202\",\"version\":" + readVersion + "}")
				.patch("/api/v1/guardians/" + guardian + "/phone").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.version", equalTo(readVersion + 1));

		support.request(school).body("{\"phone\":\"+14155550888\",\"version\":" + readVersion + "}")
				.patch("/api/v1/guardians/" + guardian + "/phone").then()
				.statusCode(HttpStatus.CONFLICT.value())
				.body("error.code", equalTo("VERSION_CONFLICT"));

		support.request(school).get("/api/v1/guardians/" + guardian).then()
				.body("data.phone", equalTo("+14155550202"));
	}

	@Test
	void classroom_staleEdit_isRejected() {
		String classroom = support.createClassroom(school, "CLS-CC");
		int readVersion = support.request(school).get("/api/v1/classrooms/" + classroom).then()
				.statusCode(HttpStatus.OK.value()).extract().path("data.version");

		support.request(school).body("{\"capacity\":35,\"version\":" + readVersion + "}")
				.patch("/api/v1/classrooms/" + classroom + "/capacity").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.version", equalTo(readVersion + 1));

		support.request(school).body("{\"capacity\":50,\"version\":" + readVersion + "}")
				.patch("/api/v1/classrooms/" + classroom + "/capacity").then()
				.statusCode(HttpStatus.CONFLICT.value())
				.body("error.code", equalTo("VERSION_CONFLICT"));

		support.request(school).get("/api/v1/classrooms/" + classroom).then()
				.body("data.capacity", equalTo(35));
	}

	// Thresholds are child rows: the scale's own version has to advance for the next editor to notice.
	@Test
	void gradingScale_thresholdOnlyEdit_advancesTheScalesVersion() {
		String thresholds = "[{\"letter\":\"A\",\"minPercentage\":50,\"points\":4},"
				+ "{\"letter\":\"F\",\"minPercentage\":0,\"points\":0}]";
		String scale = support.request(school)
				.body("{\"name\":\"Scale " + UUID.randomUUID().toString().substring(0, 6)
						+ "\",\"isDefault\":false,\"thresholds\":" + thresholds + "}")
				.post("/api/v1/grading-scales").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		int readVersion = support.request(school).get("/api/v1/grading-scales/" + scale).then()
				.statusCode(HttpStatus.OK.value()).extract().path("data.version");

		support.request(school).body("{\"thresholds\":" + thresholds + ",\"version\":" + readVersion + "}")
				.patch("/api/v1/grading-scales/" + scale + "/thresholds").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.version", equalTo(readVersion + 1));

		support.request(school).body("{\"thresholds\":" + thresholds + ",\"version\":" + readVersion + "}")
				.patch("/api/v1/grading-scales/" + scale + "/thresholds").then()
				.statusCode(HttpStatus.CONFLICT.value())
				.body("error.code", equalTo("VERSION_CONFLICT"));
	}

	@Test
	void edit_withoutAVersion_isAValidationError() {
		String student = support.createStudent(school);

		// noFilters(): the test client would otherwise fill in the current version.
		support.request(school).noFilters().body("{\"phone\":\"+14155550101\"}")
				.patch("/api/v1/students/" + student + "/phone").then()
				.statusCode(HttpStatus.BAD_REQUEST.value())
				.body("error.code", equalTo("VALIDATION_ERROR"));
	}

	@Test
	void command_needsNoVersion() {
		String student = support.createStudent(school);

		support.request(school).body("{\"reason\":\"Fee default\"}")
				.patch("/api/v1/students/" + student + "/suspend").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.enrollmentStatus", equalTo("SUSPENDED"));
	}
}
