package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import java.time.LocalDate;
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
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** An enquiry from first contact to an admission application, and the ways it can end instead. */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnquiryE2ETest extends SchoolIntegrationTestBase {

	private static final String BASE = "/api/v1/enquiries";

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;

	private School school;
	private String counsellor;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Enquiry School");
		counsellor = support.createEmployee(school, "Cora");
	}

	@Test
	void enquiry_followedUpThenConverted_becomesAnApplication() {
		String enquiry = receive("Noa", "WEBSITE").then().statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("NEW")).extract().path("data.publicId");
		LocalDate next = LocalDate.now().plusDays(3);

		admin().body("{\"employeePublicId\":\"" + counsellor + "\"}").put(BASE + "/" + enquiry + "/assignee").then()
				.statusCode(HttpStatus.OK.value()).body("data.assignedEmployee.name", equalTo("Cora Staff"));
		admin().body("{\"channel\":\"CALL\",\"note\":\"Wants a school tour\",\"nextFollowUpOn\":\"" + next + "\"}")
				.post(BASE + "/" + enquiry + "/follow-ups").then().statusCode(HttpStatus.CREATED.value());
		admin().get(BASE + "/" + enquiry).then().body("data.status", equalTo("IN_PROGRESS"))
				.body("data.nextFollowUpOn", equalTo(next.toString()));
		admin().get(BASE + "?followUpDueBy=" + next + "&size=100").then()
				.body("data.content.publicId", hasItem(enquiry));
		admin().get(BASE + "/" + enquiry + "/follow-ups").then().body("data.content", hasSize(1));

		String admission = admin().body("{\"childDateOfBirth\":\"2019-04-02\",\"childGender\":\"FEMALE\"}")
				.post(BASE + "/" + enquiry + "/convert").then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("CONVERTED"))
				.body("data.admissionPublicId", notNullValue())
				.extract().path("data.admissionPublicId");
		admin().get("/api/v1/admissions/" + admission).then().statusCode(HttpStatus.OK.value())
				.body("data.applicantFirstName", equalTo("Noa"))
				.body("data.appliedGrade", equalTo("3"));
		admin().body("{}").post(BASE + "/" + enquiry + "/convert").then().statusCode(HttpStatus.BAD_REQUEST.value());
		admin().get(BASE + "?followUpDueBy=" + next + "&size=100").then()
				.body("data.content.publicId", not(hasItem(enquiry)));
	}

	@Test
	void closedEnquiry_canBeReopened_butNotConverted() {
		String enquiry = receive("Ivy", "REFERRAL").then().extract().path("data.publicId");

		admin().body("{\"reason\":\"Moved city\"}").post(BASE + "/" + enquiry + "/close").then()
				.statusCode(HttpStatus.OK.value()).body("data.status", equalTo("CLOSED"));
		admin().body("{}").post(BASE + "/" + enquiry + "/convert").then().statusCode(HttpStatus.BAD_REQUEST.value());
		admin().post(BASE + "/" + enquiry + "/reopen").then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("IN_PROGRESS"));
	}

	@Test
	void list_filtersBySourceAndName() {
		String walkIn = receive("Zed", "WALK_IN").then().extract().path("data.publicId");

		admin().get(BASE + "?source=WALK_IN&q=zed").then().statusCode(HttpStatus.OK.value())
				.body("data.content.publicId", hasItem(walkIn));
		admin().get(BASE + "?source=PHONE&q=zed").then().body("data.content.publicId", not(hasItem(walkIn)));
	}

	@Test
	void receive_withoutPhoneOrEmail_returns400() {
		admin().body(
				"{\"childFirstName\":\"A\",\"childLastName\":\"B\",\"gradeSought\":\"1\",\"guardianFirstName\":\"C\","
						+ "\"guardianLastName\":\"D\",\"source\":\"PHONE\"}")
				.post(BASE).then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void enquiries_asATeacher_returns403() {
		support.requestWithRole(school.tenantId(), "TEACHER").get(BASE).then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void enquiries_withoutLogin_returns401() {
		given().header("X-Tenant-ID", school.tenantId()).get(BASE).then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void anotherSchool_cannotReachThisSchoolsEnquiries() {
		String enquiry = receive("Kai", "PHONE").then().extract().path("data.publicId");
		School other = support.provisionSchool("Other Enquiry School");

		support.request(other).get(BASE + "/" + enquiry).then().statusCode(HttpStatus.NOT_FOUND.value());
		support.request(other).get(BASE + "?size=100").then().body("data.content.publicId", not(hasItem(enquiry)));
		support.request(other).body("{}").post(BASE + "/" + enquiry + "/convert").then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	private Response receive(String child, String source) {
		return admin().body("{\"childFirstName\":\"" + child + "\",\"childLastName\":\"Lee\",\"gradeSought\":\"3\","
				+ "\"guardianFirstName\":\"Ola\",\"guardianLastName\":\"Lee\",\"guardianPhone\":\"+14155552671\","
				+ "\"guardianEmail\":\"ola-" + UUID.randomUUID().toString().substring(0, 6) + "@school.test\","
				+ "\"source\":\"" + source + "\"}").post(BASE);
	}

	private RequestSpecification admin() {
		return support.request(school);
	}
}
