package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.nio.charset.StandardCharsets;
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
import io.restassured.response.ValidatableResponse;

/**
 * What a student profile screen needs from the API: gender, the guardians with their relationship and custody
 * flags, the classroom the student currently sits in, and a list filtered by classroom.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StudentProfileE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;

	private School school;
	private School otherSchool;
	private String academicYear;
	private String classroomA;
	private String classroomB;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Profile School");
		otherSchool = support.provisionSchool("Other Profile School");
		academicYear = support.currentAcademicYearPublicId(school);
		classroomA = createClassroomInCurrentYear("PRF-A");
		classroomB = createClassroomInCurrentYear("PRF-B");
	}

	private String createClassroomInCurrentYear(String classCode) {
		return support.request(school)
				.body("{\"classCode\":\"" + classCode + "\",\"grade\":\"Grade 6\",\"section\":\"" + classCode
						+ "\",\"academicYearPublicId\":\"" + academicYear + "\"}")
				.post("/api/v1/classrooms").then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String enroll(String firstName, String gender) {
		String genderField = gender == null ? "" : ",\"gender\":\"" + gender + "\"";
		return support.request(school)
				.body("{\"firstName\":\"" + firstName + "\",\"lastName\":\"Profile\",\"email\":\"" + firstName
						.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6)
						+ "@school.test\",\"dateOfBirth\":\"2012-03-04\"" + genderField + "}")
				.post("/api/v1/students").then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private void placeInClassroom(String classroom, String student) {
		support.request(school)
				.body("{\"studentPublicId\":\"" + student + "\",\"academicYearPublicId\":\"" + academicYear + "\"}")
				.post("/api/v1/classrooms/" + classroom + "/students")
				.then().statusCode(HttpStatus.CREATED.value());
	}

	private String createGuardian(String firstName) {
		return support.request(school)
				.body("{\"firstName\":\"" + firstName + "\",\"lastName\":\"Guardian\",\"email\":\"" + firstName
						.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6)
						+ "@school.test\",\"phone\":\"+14155552671\"}")
				.post("/api/v1/guardians").then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private void link(String guardian, String student, String relationship, boolean primary) {
		support.request(school)
				.body("{\"studentPublicId\":\"" + student + "\",\"relationshipType\":\"" + relationship
						+ "\",\"primaryContact\":" + primary + "}")
				.post("/api/v1/guardians/" + guardian + "/students")
				.then().statusCode(HttpStatus.CREATED.value());
	}

	// ------------------------------------------------------------------ gender

	@Test
	void enroll_withGender_returnsItOnTheStudent() {
		String student = enroll("Gina", "FEMALE");

		support.request(school).get("/api/v1/students/" + student)
				.then().statusCode(HttpStatus.OK.value()).body("data.gender", equalTo("FEMALE"));
	}

	@Test
	void enroll_withoutGender_isNotSpecified() {
		String student = enroll("Nora", null);

		support.request(school).get("/api/v1/students/" + student)
				.then().body("data.gender", equalTo("NOT_SPECIFIED"));
	}

	@Test
	void enroll_withUnknownGender_returns400() {
		support.request(school)
				.body("{\"firstName\":\"Bad\",\"lastName\":\"Gender\",\"dateOfBirth\":\"2012-03-04\",\"gender\":\"ROBOT\"}")
				.post("/api/v1/students")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void updateProfile_changesTheGender() {
		String student = enroll("Gus", "MALE");
		Integer version = support.request(school).get("/api/v1/students/" + student).path("data.version");

		support.request(school)
				.body("{\"firstName\":\"Gus\",\"lastName\":\"Profile\",\"dateOfBirth\":\"2012-03-04\","
						+ "\"gender\":\"OTHER\",\"version\":" + version + "}")
				.patch("/api/v1/students/" + student + "/contact-details")
				.then().statusCode(HttpStatus.OK.value()).body("data.gender", equalTo("OTHER"));
	}

	@Test
	void updateProfile_withoutGender_returns400() {
		String student = enroll("Gwen", "FEMALE");

		support.request(school)
				.body("{\"firstName\":\"Gwen\",\"lastName\":\"Profile\",\"dateOfBirth\":\"2012-03-04\"}")
				.patch("/api/v1/students/" + student + "/contact-details")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void bulkImport_readsTheOptionalGenderColumn() {
		String code = "GEN-" + UUID.randomUUID().toString().substring(0, 6);
		String csv = "studentCode,firstName,lastName,email,dateOfBirth,gender\n" + code
				+ ",Csv,Gender,,2011-01-01,female\n";

		support.request(school)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType("multipart/form-data")
				.multiPart("file", "students.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
				.post("/api/v1/students/bulk-import")
				.then().statusCode(HttpStatus.OK.value()).body("data.successCount", equalTo(1));

		support.request(school).queryParam("q", code).get("/api/v1/students")
				.then().body("data.content[0].gender", equalTo("FEMALE"));
	}

	// -------------------------------------------------------------- guardians

	@Test
	void guardians_listsEveryLinkedGuardianPrimaryFirstWithRelationshipAndCustody() {
		String student = enroll("Gwyn", "FEMALE");
		String father = createGuardian("Frank");
		String mother = createGuardian("Mary");
		link(father, student, "FATHER", false);
		link(mother, student, "MOTHER", true);
		support.request(school)
				.body("{\"note\":\"Court order of 2026-01-10\"}")
				.patch("/api/v1/guardians/" + father + "/students/" + student + "/custody-restriction/apply")
				.then().statusCode(HttpStatus.OK.value());

		support.request(school).get("/api/v1/students/" + student + "/guardians")
				.then().statusCode(HttpStatus.OK.value())
				.body("data", hasSize(2))
				.body("data[0].guardianPublicId", equalTo(mother))
				.body("data[0].relationshipType", equalTo("MOTHER"))
				.body("data[0].primaryContact", equalTo(true))
				.body("data[0].custodyRestricted", equalTo(false))
				.body("data[0].phone", notNullValue())
				.body("data[1].guardianPublicId", equalTo(father))
				.body("data[1].relationshipType", equalTo("FATHER"))
				.body("data[1].primaryContact", equalTo(false))
				.body("data[1].custodyRestricted", equalTo(true))
				.body("data[1].custodyRestrictionNote", equalTo("Court order of 2026-01-10"))
				.body("data[1].linkPublicId", notNullValue());
	}

	@Test
	void guardians_ofAStudentWithNone_isEmpty() {
		String student = enroll("Solo", null);

		support.request(school).get("/api/v1/students/" + student + "/guardians")
				.then().statusCode(HttpStatus.OK.value()).body("data", empty());
	}

	@Test
	void guardians_ofAnUnknownStudent_returns404() {
		support.request(school).get("/api/v1/students/" + UUID.randomUUID() + "/guardians")
				.then().statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void guardians_ofAnotherTenantsStudent_returns404() {
		String student = enroll("Isolated", null);

		support.request(otherSchool).get("/api/v1/students/" + student + "/guardians")
				.then().statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void guardians_withoutStudentRead_returns403() {
		String student = enroll("Guarded", null);

		support.requestWithRole(school.tenantId(), "PARENT").get("/api/v1/students/" + student + "/guardians")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void guardians_withoutLogin_returns401() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId())
				.get("/api/v1/students/" + UUID.randomUUID() + "/guardians")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	// -------------------------------------------------------------- placement

	@Test
	void currentClassroom_isTheClassroomOfTheCurrentAcademicYear() {
		String student = enroll("Placed", null);
		placeInClassroom(classroomA, student);

		support.request(school).get("/api/v1/students/" + student)
				.then().statusCode(HttpStatus.OK.value())
				.body("data.currentClassroom.publicId", equalTo(classroomA))
				.body("data.currentClassroom.name", equalTo("Grade 6 PRF-A"))
				.body("data.currentClassroom.academicYearPublicId", equalTo(academicYear))
				.body("data.currentClassroom.rollNumber", nullValue());
	}

	@Test
	void currentClassroom_carriesTheRollNumber() {
		String student = enroll("Numbered", null);
		placeInClassroom(classroomA, student);
		support.request(school).body("{\"rollNumber\":\"17\"}")
				.patch("/api/v1/classrooms/" + classroomA + "/students/" + student + "/roll-number")
				.then().statusCode(HttpStatus.OK.value());

		support.request(school).get("/api/v1/students/" + student)
				.then().body("data.currentClassroom.rollNumber", equalTo("17"));
	}

	@Test
	void currentClassroom_isNullForAStudentWhoIsNotPlaced() {
		String student = enroll("Unplaced", null);

		support.request(school).get("/api/v1/students/" + student)
				.then().body("data.currentClassroom", nullValue());
	}

	@Test
	void list_showsEachStudentsClassroomInOnePage() {
		String inA = enroll("ListA", null);
		String inB = enroll("ListB", null);
		String nowhere = enroll("ListNone", null);
		placeInClassroom(classroomA, inA);
		placeInClassroom(classroomB, inB);

		ValidatableResponse page = support.request(school).queryParam("size", 100).queryParam("q", "List")
				.get("/api/v1/students").then().statusCode(HttpStatus.OK.value());

		page.body("data.content.find { it.publicId == '" + inA + "' }.currentClassroom.publicId", equalTo(classroomA));
		page.body("data.content.find { it.publicId == '" + inB + "' }.currentClassroom.publicId", equalTo(classroomB));
		page.body("data.content.find { it.publicId == '" + nowhere + "' }.currentClassroom", nullValue());
	}

	// ----------------------------------------------------------------- filter

	@Test
	void list_filteredByClassroom_returnsOnlyItsStudents() {
		String inA = enroll("FilterIn", null);
		String inB = enroll("FilterOut", null);
		placeInClassroom(classroomA, inA);
		placeInClassroom(classroomB, inB);

		support.request(school).queryParam("classroomPublicId", classroomA).queryParam("q", "Filter")
				.get("/api/v1/students")
				.then().statusCode(HttpStatus.OK.value())
				.body("data.content.publicId", contains(inA))
				.body("data.totalElements", equalTo(1));
	}

	@Test
	void list_filteredByClassroomAndStatus_composesBothFilters() {
		String active = enroll("ComposeActive", null);
		String withdrawn = enroll("ComposeGone", null);
		placeInClassroom(classroomB, active);
		placeInClassroom(classroomB, withdrawn);
		support.request(school).patch("/api/v1/students/" + withdrawn + "/withdraw")
				.then().statusCode(HttpStatus.OK.value());

		support.request(school).queryParam("classroomPublicId", classroomB).queryParam("status", "ACTIVE")
				.queryParam("q", "Compose").get("/api/v1/students")
				.then().body("data.content.publicId", contains(active));
	}

	@Test
	void list_filteredByAnUnknownClassroom_returns404() {
		support.request(school).queryParam("classroomPublicId", UUID.randomUUID()).get("/api/v1/students")
				.then().statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void list_filteredByAnotherTenantsClassroom_returns404() {
		support.request(otherSchool).queryParam("classroomPublicId", classroomA).get("/api/v1/students")
				.then().statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void list_filteredByAMalformedClassroomId_returns400() {
		support.request(school).queryParam("classroomPublicId", "not-a-uuid").get("/api/v1/students")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}
}
