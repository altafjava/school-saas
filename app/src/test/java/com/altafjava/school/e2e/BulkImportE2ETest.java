package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Each import type through upload, mapping, dry run and import, as a school moving onto the system would. */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BulkImportE2ETest extends SchoolIntegrationTestBase {

	private static final String BASE = "/api/v1/imports";

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;

	private School school;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Import School");
	}

	@Test
	void students_dryRunReportsEveryProblem_andTheImportSkipsThem() {
		String code = "IMP-" + suffix();
		String job = upload("STUDENTS", "Admission No,First Name,Last Name,DOB\n"
				+ code + "-1,Ada,Lee,2015-04-30\n"
				+ code + "-2,Ben,,2015-05-01\n"
				+ code + "-1,Cy,Lee,2015-06-01\n"
				+ code + "-4,Dee,Lee,30/04/2015\n")
				.then().statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("UPLOADED"))
				.body("data.totalRows", equalTo(4))
				.body("data.mapping.firstName", equalTo("First Name"))
				.body("data.preview", hasSize(4))
				.extract().path("data.publicId");

		admin().body("{\"mapping\":{\"studentCode\":\"Admission No\",\"firstName\":\"First Name\","
				+ "\"lastName\":\"Last Name\",\"dateOfBirth\":\"DOB\"}}").put(BASE + "/" + job + "/mapping").then()
				.statusCode(HttpStatus.OK.value());
		admin().post(BASE + "/" + job + "/dry-run").then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("VALIDATED"))
				.body("data.validRows", equalTo(1))
				.body("data.invalidRows", equalTo(3));
		admin().get(BASE + "/" + job + "/errors").then()
				.body("data.content.lineNumber", equalTo(java.util.List.of(3, 4, 5)))
				.body("data.content.find { it.lineNumber == 3 }.field", equalTo("lastName"))
				.body("data.content.find { it.lineNumber == 4 }.message", containsString("Repeats line 2"));
		admin().get("/api/v1/students?q=" + code + "&size=50").then().body("data.content", hasSize(0));

		admin().post(BASE + "/" + job + "/execute").then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("COMPLETED"))
				.body("data.importedRows", equalTo(1))
				.body("data.failedRows", equalTo(3));
		admin().get("/api/v1/students?q=" + code + "&size=50").then().body("data.content", hasSize(1));
		admin().post(BASE + "/" + job + "/execute").then().statusCode(HttpStatus.CONFLICT.value());

		Response report = admin().get(BASE + "/" + job + "/error-report");
		report.then().statusCode(HttpStatus.OK.value()).contentType(containsString("text/csv"));
		org.assertj.core.api.Assertions.assertThat(report.asString()).startsWith("line,field,message\n3,");
	}

	@Test
	void mapping_withoutARequiredField_isRefused() {
		String job = upload("STUDENTS", "Name,Surname\nAda,Lee\n").then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		admin().post(BASE + "/" + job + "/dry-run").then().statusCode(HttpStatus.BAD_REQUEST.value())
				.body("error.message", containsString("firstName"));
		admin().body("{\"mapping\":{\"firstName\":\"Name\",\"lastName\":\"Nope\"}}").put(BASE + "/" + job + "/mapping")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void staff_guardians_andOpeningBalances_comeInThroughTheSameSteps() {
		String code = "FAM-" + suffix();
		importClean("STUDENTS", "studentCode,firstName,lastName\n" + code + "-1,Ada,Lee\n" + code + "-2,Ben,Lee\n");

		importClean("STAFF", "employeeCode,firstName,lastName,email,staffCategory,joinDate\n"
				+ "T-" + suffix() + ",Tia,Rao,tia-" + suffix() + "@school.test,TEACHING,2024-06-01\n"
				+ "A-" + suffix() + ",Avi,Rao,avi-" + suffix() + "@school.test,administrative,2024-06-01\n");

		String parentEmail = "mom-" + suffix() + "@school.test";
		importClean("GUARDIANS", "studentCode,firstName,lastName,phone,email,relationship,primaryContact\n"
				+ code + "-1,Mia,Lee,+14155552671," + parentEmail + ",MOTHER,yes\n"
				+ code + "-2,Mia,Lee,+14155552671," + parentEmail + ",MOTHER,yes\n");
		String student = admin().get("/api/v1/students?q=" + code + "-2").then().extract()
				.path("data.content[0].publicId");
		admin().get("/api/v1/students/" + student + "/guardians").then().statusCode(HttpStatus.OK.value())
				.body("data", hasSize(1));

		importClean("OPENING_FEE_BALANCES", "studentCode,amount,dueDate\n" + code + "-1,2500.50,2026-12-31\n");
		String first = admin().get("/api/v1/students?q=" + code + "-1").then().extract()
				.path("data.content[0].publicId");
		admin().get("/api/v1/students/" + first + "/fee-balance").then().statusCode(HttpStatus.OK.value())
				.body("data.find { it.feeStructureName == 'Opening balance' }.outstandingBalance", equalTo(2500.5F));

		String again = upload("OPENING_FEE_BALANCES", "studentCode,amount\n" + code + "-1,10\n").then()
				.extract().path("data.publicId");
		admin().post(BASE + "/" + again + "/dry-run").then().body("data.invalidRows", equalTo(1));
	}

	@Test
	void history_listsEveryUpload() {
		String job = upload("STAFF", "firstName\nZed\n").then().extract().path("data.publicId");

		admin().get(BASE + "?type=STAFF&size=100").then().statusCode(HttpStatus.OK.value())
				.body("data.content.publicId", hasItem(job))
				.body("data.content.find { it.publicId == '" + job + "' }.completedAt", nullValue());
		admin().get(BASE + "/types").then().body("data.type", hasItem("OPENING_FEE_BALANCES"));
	}

	@Test
	void imports_asATeacher_returns403() {
		support.requestWithRole(school.tenantId(), "TEACHER").get(BASE).then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void imports_withoutLogin_returns401() {
		given().header("X-Tenant-ID", school.tenantId()).get(BASE).then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void anotherSchool_cannotReachThisSchoolsImports() {
		String job = upload("STUDENTS", "firstName,lastName\nAda,Lee\n").then().extract().path("data.publicId");
		School other = support.provisionSchool("Other Import School");

		support.request(other).get(BASE + "/" + job).then().statusCode(HttpStatus.NOT_FOUND.value());
		support.request(other).post(BASE + "/" + job + "/execute").then().statusCode(HttpStatus.NOT_FOUND.value());
		support.request(other).get(BASE + "?size=100").then().body("data.content.publicId", not(hasItem(job)));
	}

	private void importClean(String type, String csv) {
		String job = upload(type, csv).then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		admin().post(BASE + "/" + job + "/dry-run").then().statusCode(HttpStatus.OK.value())
				.body("data.invalidRows", equalTo(0));
		admin().post(BASE + "/" + job + "/execute").then().statusCode(HttpStatus.OK.value())
				.body("data.failedRows", equalTo(0));
	}

	private Response upload(String type, String csv) {
		return given().header("X-Tenant-ID", school.tenantId())
				.header("Authorization", "Bearer " + school.adminToken())
				.multiPart("file", "upload.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
				.queryParam("type", type)
				.post(BASE);
	}

	private RequestSpecification admin() {
		return support.request(school);
	}

	private static String suffix() {
		return UUID.randomUUID().toString().substring(0, 6);
	}
}
