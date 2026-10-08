package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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

@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentSiblingE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;

	private School school;

	@BeforeAll
	void setUp() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Sibling School");
	}

	private void link(String studentPublicId, String siblingPublicId) {
		support.request(school).body("{\"siblingPublicId\":\"" + siblingPublicId + "\"}")
				.post("/api/v1/students/" + studentPublicId + "/siblings")
				.then().statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void linking_isTransitiveAndLeavingRemovesOnlyThatStudent() {
		String alice = support.createStudent(school);
		String bob = support.createStudent(school);
		String cara = support.createStudent(school);

		link(alice, bob);
		support.request(school).get("/api/v1/students/" + alice + "/siblings")
				.then().statusCode(HttpStatus.OK.value()).body("data.publicId", containsInAnyOrder(bob));

		link(bob, cara);
		support.request(school).get("/api/v1/students/" + alice + "/siblings")
				.then().body("data.publicId", containsInAnyOrder(bob, cara));
		support.request(school).get("/api/v1/students/" + cara + "/siblings")
				.then().body("data.publicId", containsInAnyOrder(alice, bob));

		support.request(school).delete("/api/v1/students/" + bob + "/siblings")
				.then().statusCode(HttpStatus.NO_CONTENT.value());
		support.request(school).get("/api/v1/students/" + alice + "/siblings")
				.then().body("data.publicId", containsInAnyOrder(cara));
		support.request(school).get("/api/v1/students/" + bob + "/siblings")
				.then().body("data", empty());

		support.request(school).delete("/api/v1/students/" + cara + "/siblings")
				.then().statusCode(HttpStatus.NO_CONTENT.value());
		support.request(school).get("/api/v1/students/" + alice + "/siblings")
				.then().body("data", empty());
	}

	@Test
	void linkingTwoFamilies_mergesThem() {
		String a = support.createStudent(school);
		String b = support.createStudent(school);
		String c = support.createStudent(school);
		String d = support.createStudent(school);
		link(a, b);
		link(c, d);

		link(a, c);

		support.request(school).get("/api/v1/students/" + d + "/siblings")
				.then().body("data.publicId", containsInAnyOrder(a, b, c));
	}

	@Test
	void linking_alreadyLinkedOrSelf_returns400() {
		String a = support.createStudent(school);
		String b = support.createStudent(school);
		link(a, b);

		support.request(school).body("{\"siblingPublicId\":\"" + b + "\"}")
				.post("/api/v1/students/" + a + "/siblings").then().statusCode(HttpStatus.BAD_REQUEST.value());
		support.request(school).body("{\"siblingPublicId\":\"" + a + "\"}")
				.post("/api/v1/students/" + a + "/siblings").then().statusCode(HttpStatus.BAD_REQUEST.value());
		support.request(school).delete("/api/v1/students/" + support.createStudent(school) + "/siblings")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void suggestions_listStudentsSharingAGuardianUntilTheyAreLinked() {
		String older = support.createStudent(school);
		String younger = support.createStudent(school);
		String guardianPublicId = support.request(school)
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane-" + UUID.randomUUID()
						+ "@school.test\",\"phone\":\"+14155552671\"}")
				.post("/api/v1/guardians").then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		for (String child : new String[] { older, younger }) {
			support.request(school)
					.body("{\"studentPublicId\":\"" + child + "\",\"relationshipType\":\"MOTHER\","
							+ "\"primaryContact\":true}")
					.post("/api/v1/guardians/" + guardianPublicId + "/students")
					.then().statusCode(HttpStatus.CREATED.value());
		}

		support.request(school).get("/api/v1/students/" + older + "/siblings/suggestions")
				.then().statusCode(HttpStatus.OK.value()).body("data.publicId", hasItem(younger));

		link(older, younger);
		support.request(school).get("/api/v1/students/" + older + "/siblings/suggestions")
				.then().body("data", hasSize(0));
	}

	@Test
	void teacher_canListButNotLinkSiblings() {
		String a = support.createStudent(school);
		String b = support.createStudent(school);

		support.requestWithRole(school.tenantId(), "TEACHER").get("/api/v1/students/" + a + "/siblings")
				.then().statusCode(HttpStatus.OK.value());
		support.requestWithRole(school.tenantId(), "TEACHER").body("{\"siblingPublicId\":\"" + b + "\"}")
				.post("/api/v1/students/" + a + "/siblings").then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void siblings_withoutJwt_return401() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId())
				.get("/api/v1/students/" + UUID.randomUUID() + "/siblings")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void linking_aStudentFromAnotherSchool_returns404() {
		String mine = support.createStudent(school);
		School other = support.provisionSchool("Other Sibling School");
		String theirs = support.createStudent(other);

		support.request(school).body("{\"siblingPublicId\":\"" + theirs + "\"}")
				.post("/api/v1/students/" + mine + "/siblings").then().statusCode(HttpStatus.NOT_FOUND.value());
	}
}
