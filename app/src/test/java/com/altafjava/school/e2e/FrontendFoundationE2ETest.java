package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.demo.DemoDataSeeder;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * What a frontend needs on day one, exercised against the demo school: the session bootstrap
 * ({@code /me}, {@code /me/school-profile}), {@code q} search, dropdown lookups and the generic
 * custom-field endpoint.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrontendFoundationE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private DemoDataSeeder demoDataSeeder;

	private Long tenantId;

	@BeforeAll
	void seedDemoSchool() {
		Tenant tenant = demoDataSeeder.seed();
		tenantId = tenant.getId();
	}

	private RequestSpecification as(String email) {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String token = given().header("X-Tenant-ID", tenantId).contentType(ContentType.JSON)
				.body(Map.of("email", email, "password", DemoDataSeeder.PASSWORD))
				.post("/api/v1/auth/login").then().statusCode(200).extract().path("data.accessToken");
		return given().header("X-Tenant-ID", tenantId).header("Authorization", "Bearer " + token)
				.contentType(ContentType.JSON);
	}

	@Test
	void seeding_isIdempotent() {
		Tenant again = demoDataSeeder.seed();

		org.junit.jupiter.api.Assertions.assertEquals(tenantId, again.getId());
	}

	@Test
	void me_forATeacher_carriesRoleAndSchoolPermissions() {
		as("teacher@demo.school").get("/api/v1/me").then().statusCode(200)
				.body("data.roles", hasItem("TEACHER"))
				.body("data.permissions", hasItem("CLASSROOM_READ"))
				.body("data.permissions", not(hasItem("TENANT_DELETE")))
				.body("data.tenant.subdomain", equalTo("demo"));
	}

	@Test
	void schoolProfile_forATeacher_isAStaffMemberAndTeacher() {
		as("teacher@demo.school").get("/api/v1/me/school-profile").then().statusCode(200)
				.body("data.teacher", equalTo(true))
				.body("data.employeeId", notNullValue())
				.body("data.guardianId", org.hamcrest.Matchers.nullValue());
	}

	@Test
	void schoolProfile_forAParent_listsLinkedChildren() {
		as("parent@demo.school").get("/api/v1/me/school-profile").then().statusCode(200)
				.body("data.guardianId", notNullValue())
				.body("data.linkedStudents.size()", greaterThanOrEqualTo(1))
				.body("data.linkedStudents[0].studentCode", notNullValue());
	}

	@Test
	void schoolProfile_forAStudent_hasItsStudentRecord() {
		as("student@demo.school").get("/api/v1/me/school-profile").then().statusCode(200)
				.body("data.studentId", notNullValue())
				.body("data.teacher", equalTo(false));
	}

	@Test
	void students_searchByName_filtersAndKeepsPaging() {
		RequestSpecification admin = as(DemoDataSeeder.ADMIN_EMAIL);

		admin.queryParam("q", "aarav").queryParam("size", 5).get("/api/v1/students").then().statusCode(200)
				.body("data.content.size()", greaterThanOrEqualTo(1))
				.body("data.content.firstName", org.hamcrest.Matchers.everyItem(equalTo("Aarav")));
		admin.queryParam("q", "zzz-nobody").get("/api/v1/students").then().statusCode(200)
				.body("data.content", empty())
				.body("data.totalElements", equalTo(0));
	}

	@Test
	void students_searchTreatsPercentLiterally() {
		as(DemoDataSeeder.ADMIN_EMAIL).queryParam("q", "%").get("/api/v1/students").then().statusCode(200)
				.body("data.content", empty());
	}

	@Test
	void lookups_returnBoundedOptions() {
		RequestSpecification admin = as(DemoDataSeeder.ADMIN_EMAIL);

		admin.queryParam("limit", 3).get("/api/v1/lookups/classrooms").then().statusCode(200)
				.body("data", hasSize(3))
				.body("data[0].id", notNullValue())
				.body("data[0].label", notNullValue());
		admin.queryParam("q", "tuition").get("/api/v1/lookups/fee-structures").then().statusCode(200)
				.body("data.label", hasItem("Tuition"));
	}

	@Test
	void lookups_areLimitedByTheCallersPermissions() {
		RequestSpecification teacher = as("teacher@demo.school");

		teacher.get("/api/v1/lookups").then().statusCode(200)
				.body("data", hasItem("classrooms"))
				.body("data", not(hasItem("departments")));
		teacher.get("/api/v1/lookups/departments").then().statusCode(403);
	}

	@Test
	void customFields_genericEndpoint_acceptsEveryEntityTypeSpelling() {
		RequestSpecification admin = as(DemoDataSeeder.ADMIN_EMAIL);
		String teacherId = admin.get("/api/v1/teachers").then().statusCode(200).extract()
				.path("data.content[0].publicId");

		admin.get("/api/v1/custom-fields/TEACHER/" + teacherId).then().statusCode(200);
		admin.get("/api/v1/custom-fields/teacher/" + teacherId).then().statusCode(200);
		admin.get("/api/v1/custom-fields/teacher/" + java.util.UUID.randomUUID()).then().statusCode(404);
		admin.get("/api/v1/custom-fields/bogus/" + teacherId).then().statusCode(400)
				.body("error.code", equalTo("INVALID_REQUEST"));
	}
}
