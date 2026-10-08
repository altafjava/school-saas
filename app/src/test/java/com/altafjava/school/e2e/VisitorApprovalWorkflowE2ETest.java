package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import com.altafjava.school.util.TestPhotos;
import io.restassured.RestAssured;

// Campus security: nobody is checked in without an approved request and a photo, and the badge dies at check-out.
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VisitorApprovalWorkflowE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private TestPhotos testPhotos;

	private School school;
	private String hostPublicId;
	private Long hostUserId;
	private Long strangerUserId;

	@BeforeAll
	void setUp() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Visitor Security School");
		hostPublicId = support.createEmployee(school, "Hal");
		hostUserId = support.createUserWithRole(school.tenantId(), "host-" + UUID.randomUUID() + "@school.test",
				"TEACHER");
		support.linkEmployeeToUser(school, hostPublicId, hostUserId);
		String strangerPublicId = support.createEmployee(school, "Sid");
		strangerUserId = support.createUserWithRole(school.tenantId(), "stranger-" + UUID.randomUUID() + "@school.test",
				"TEACHER");
		support.linkEmployeeToUser(school, strangerPublicId, strangerUserId);
	}

	private String walkIn(String visitorName) {
		return support.request(school)
				.body("{\"visitorName\":\"" + visitorName + "\",\"visitorPhone\":\"555-0100\","
						+ "\"purpose\":\"Parent-teacher meeting\",\"hostEmployeePublicId\":\"" + hostPublicId + "\"}")
				.post("/api/v1/visitor-requests")
				.then().statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("PENDING"))
				.body("data.source", equalTo("WALK_IN"))
				.extract().path("data.publicId");
	}

	private String photo() {
		return testPhotos.store(school.tenantId()).toString();
	}

	private io.restassured.response.ValidatableResponse checkIn(String requestPublicId, String photoPublicId) {
		String photoField = photoPublicId == null ? "" : ",\"photoFilePublicId\":\"" + photoPublicId + "\"";
		return support.request(school)
				.body("{\"visitorRequestPublicId\":\"" + requestPublicId + "\"" + photoField + "}")
				.post("/api/v1/visitor-logs").then();
	}

	@Test
	void walkIn_isApprovedByTheHostThenCheckedInWithAPhotoAndABadgeAndCheckedOut() {
		String requestPublicId = walkIn("Alex Ray");

		checkIn(requestPublicId, photo()).statusCode(HttpStatus.BAD_REQUEST.value());

		support.requestAsUser(school.tenantId(), hostUserId, "TEACHER")
				.get("/api/v1/visitor-requests/hosted-by-me?status=PENDING")
				.then().statusCode(HttpStatus.OK.value()).body("data.content.publicId", hasItem(requestPublicId));
		support.requestAsUser(school.tenantId(), hostUserId, "TEACHER")
				.patch("/api/v1/visitor-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.OK.value()).body("data.status", equalTo("APPROVED"));

		checkIn(requestPublicId, null).statusCode(HttpStatus.BAD_REQUEST.value());

		String logPublicId = checkIn(requestPublicId, photo()).statusCode(HttpStatus.CREATED.value())
				.body("data.badgeIssued", equalTo(true))
				.body("data.photoFilePublicId", notNullValue())
				.body("data.checkOutAt", nullValue())
				.extract().path("data.publicId");

		checkIn(requestPublicId, photo()).statusCode(HttpStatus.BAD_REQUEST.value());
		support.request(school).get("/api/v1/visitor-requests/" + requestPublicId)
				.then().body("data.status", equalTo("CHECKED_IN"));
		support.request(school).get("/api/v1/visitor-logs/" + logPublicId + "/badge")
				.then().statusCode(HttpStatus.OK.value()).contentType("application/pdf");
		support.request(school).queryParam("stillCheckedIn", true).get("/api/v1/visitor-logs")
				.then().statusCode(HttpStatus.OK.value()).body("data.content.publicId", hasItem(logPublicId));

		support.request(school).patch("/api/v1/visitor-logs/" + logPublicId + "/check-out")
				.then().statusCode(HttpStatus.OK.value()).body("data.checkOutAt", notNullValue());
		support.request(school).patch("/api/v1/visitor-logs/" + logPublicId + "/check-out")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void photoAttachedAtPreRegistration_isEnoughAtTheGate() {
		String requestPublicId = walkIn("Kim Lee");
		support.request(school).body("{\"photoFilePublicId\":\"" + photo() + "\"}")
				.patch("/api/v1/visitor-requests/" + requestPublicId + "/photo")
				.then().statusCode(HttpStatus.OK.value());
		support.request(school).patch("/api/v1/visitor-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.OK.value());

		checkIn(requestPublicId, null).statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void preRegisteredVisit_cannotBeCheckedInBeforeItsDate() {
		String requestPublicId = support.request(school)
				.body("{\"visitorName\":\"Nia Park\",\"purpose\":\"Interview\",\"hostEmployeePublicId\":\""
						+ hostPublicId + "\",\"visitDate\":\"" + LocalDate.now().plusDays(3) + "\"}")
				.post("/api/v1/visitor-requests")
				.then().statusCode(HttpStatus.CREATED.value()).body("data.source", equalTo("PRE_REGISTERED"))
				.extract().path("data.publicId");
		support.request(school).patch("/api/v1/visitor-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.OK.value());

		checkIn(requestPublicId, photo()).statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void preRegistration_forADateThatHasPassed_returns400() {
		support.request(school)
				.body("{\"visitorName\":\"Old Visit\",\"purpose\":\"x\",\"hostEmployeePublicId\":\"" + hostPublicId
						+ "\",\"visitDate\":\"" + LocalDate.now().minusDays(1) + "\"}")
				.post("/api/v1/visitor-requests").then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void rejectedRequest_cannotBeCheckedIn() {
		String requestPublicId = walkIn("Sam Fox");
		support.requestAsUser(school.tenantId(), hostUserId, "TEACHER")
				.body("{\"reason\":\"Not expecting anyone\"}")
				.patch("/api/v1/visitor-requests/" + requestPublicId + "/reject")
				.then().statusCode(HttpStatus.OK.value()).body("data.status", equalTo("REJECTED"));

		checkIn(requestPublicId, photo()).statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void anotherStaffMember_cannotDecideOnSomeoneElsesGuest() {
		String requestPublicId = walkIn("Ivy Moon");

		support.requestAsUser(school.tenantId(), strangerUserId, "TEACHER")
				.patch("/api/v1/visitor-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
		support.requestAsUser(school.tenantId(), strangerUserId, "TEACHER")
				.body("{\"reason\":\"no\"}").patch("/api/v1/visitor-requests/" + requestPublicId + "/reject")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void parentRole_cannotRaiseOrApprove() {
		support.requestWithRole(school.tenantId(), "PARENT")
				.body("{\"visitorName\":\"X\",\"purpose\":\"x\",\"hostEmployeePublicId\":\"" + hostPublicId + "\"}")
				.post("/api/v1/visitor-requests").then().statusCode(HttpStatus.FORBIDDEN.value());
		support.requestWithRole(school.tenantId(), "PARENT")
				.patch("/api/v1/visitor-requests/" + UUID.randomUUID() + "/approve")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void visitorEndpoints_withoutJwt_return401() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId()).get("/api/v1/visitor-requests")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
		RestAssured.given().header("X-Tenant-ID", school.tenantId())
				.contentType(io.restassured.http.ContentType.JSON).body("{}").post("/api/v1/visitor-logs")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void requestAndLog_areNotVisibleToAnotherTenant() {
		String requestPublicId = walkIn("Nia Park");
		School other = support.provisionSchool("Other Visitor School");

		support.request(other).get("/api/v1/visitor-requests/" + requestPublicId)
				.then().statusCode(HttpStatus.NOT_FOUND.value());
		support.request(other).patch("/api/v1/visitor-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.NOT_FOUND.value());
		support.request(other)
				.body("{\"visitorRequestPublicId\":\"" + requestPublicId + "\",\"photoFilePublicId\":\""
						+ UUID.randomUUID() + "\"}")
				.post("/api/v1/visitor-logs").then().statusCode(HttpStatus.NOT_FOUND.value());
	}
}
