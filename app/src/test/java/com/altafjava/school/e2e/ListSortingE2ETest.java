package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
class ListSortingE2ETest extends SchoolIntegrationTestBase {

	// Every list endpoint that declares its own sortable fields, so a field name that does not map
	// to a real attribute of the entity fails here rather than in front of a user.
	private static final Map<String, List<String>> DECLARED_SORT_FIELDS = new LinkedHashMap<>();

	static {
		DECLARED_SORT_FIELDS.put("/api/v1/students",
				List.of("studentCode", "firstName", "lastName", "dateOfBirth", "enrollmentStatus"));
		DECLARED_SORT_FIELDS.put("/api/v1/teachers",
				List.of("employeeCode", "firstName", "lastName", "joinDate", "status"));
		DECLARED_SORT_FIELDS.put("/api/v1/employees",
				List.of("employeeCode", "firstName", "lastName", "joinDate", "status", "staffCategory"));
		DECLARED_SORT_FIELDS.put("/api/v1/guardians", List.of("firstName", "lastName"));
		DECLARED_SORT_FIELDS.put("/api/v1/admissions",
				List.of("applicantFirstName", "applicantLastName", "appliedGrade", "status", "submittedAt"));
		DECLARED_SORT_FIELDS.put("/api/v1/exams", List.of("title", "scheduledAt", "status", "maxMarks"));
		DECLARED_SORT_FIELDS.put("/api/v1/grades", List.of("marks", "gradeLetter"));
		DECLARED_SORT_FIELDS.put("/api/v1/attendance", List.of("attendanceDate", "status"));
		DECLARED_SORT_FIELDS.put("/api/v1/fee-payments", List.of("paidAt", "paidAmount", "receiptNumber"));
		DECLARED_SORT_FIELDS.put("/api/v1/fee-structures", List.of("name", "amount", "frequency"));
		DECLARED_SORT_FIELDS.put("/api/v1/leave-requests", List.of("startDate", "endDate", "status", "daysRequested"));
		DECLARED_SORT_FIELDS.put("/api/v1/payslips", List.of("payYear", "payMonth", "status", "netPay"));
		DECLARED_SORT_FIELDS.put("/api/v1/books", List.of("title", "author", "category"));
		DECLARED_SORT_FIELDS.put("/api/v1/visitor-logs", List.of("checkInAt", "checkOutAt"));
		DECLARED_SORT_FIELDS.put("/api/v1/tickets", List.of("status", "category", "subject"));
		DECLARED_SORT_FIELDS.put("/api/v1/events", List.of("title", "eventDate"));
		DECLARED_SORT_FIELDS.put("/api/v1/classrooms", List.of("classCode", "grade", "section"));
		DECLARED_SORT_FIELDS.put("/api/v1/visitor-requests", List.of("visitDate", "status", "source"));
	}

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;

	private School school;

	@BeforeAll
	void setUp() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Sorting School");
	}

	private String enroll(String firstName, String lastName) {
		return support.request(school)
				.body("{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\",\"email\":\""
						+ firstName.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6)
						+ "@school.test\",\"dateOfBirth\":\"2010-01-01\"}")
				.post("/api/v1/students").then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	@Test
	void everyDeclaredSortField_isAcceptedByItsEndpoint() {
		DECLARED_SORT_FIELDS.forEach((path, fields) -> fields.forEach(field -> {
			for (String direction : List.of("asc", "desc")) {
				support.request(school).queryParam("sort", field + "," + direction).get(path)
						.then().statusCode(HttpStatus.OK.value());
			}
		}));
	}

	@Test
	void students_areReturnedInTheRequestedOrder() {
		String tag = UUID.randomUUID().toString().substring(0, 4);
		enroll("Charlie" + tag, "Zed");
		enroll("Alice" + tag, "Yu");
		enroll("Bob" + tag, "Zed");

		support.request(school).queryParam("sort", "firstName,desc").get("/api/v1/students")
				.then().statusCode(HttpStatus.OK.value())
				.body("data.content.firstName", contains("Charlie" + tag, "Bob" + tag, "Alice" + tag));
		support.request(school).queryParam("sort", "lastName,desc").queryParam("sort", "firstName,asc")
				.get("/api/v1/students")
				.then().body("data.content.firstName", contains("Bob" + tag, "Charlie" + tag, "Alice" + tag));
	}

	@Test
	void sorting_byAFieldTheEndpointDoesNotDeclare_returns400AndNamesTheAllowedFields() {
		support.request(school).queryParam("sort", "email,asc").get("/api/v1/students")
				.then().statusCode(HttpStatus.BAD_REQUEST.value())
				.body("success", equalTo(false))
				.body("error.message", containsString("firstName"));
	}

	@Test
	void sorting_byAnAuditColumn_worksOnAnEndpointWithNoDeclaredFields() {
		support.request(school).queryParam("sort", "createdAt,desc").get("/api/v1/academic-years")
				.then().statusCode(HttpStatus.OK.value());
	}

	@Test
	void sorting_withABadDirectionOrTooManyKeys_returns400() {
		support.request(school).queryParam("sort", "firstName,sideways").get("/api/v1/students")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
		support.request(school).queryParam("sort", "firstName").queryParam("sort", "lastName")
				.queryParam("sort", "studentCode").queryParam("sort", "createdAt").get("/api/v1/students")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void aMalformedId_isABadRequestNotAServerError() {
		support.request(school).get("/api/v1/students/not-a-uuid")
				.then().statusCode(HttpStatus.BAD_REQUEST.value())
				.body("error.code", equalTo("INVALID_ARGUMENT"));
	}

	@Test
	void everyErrorCarriesTheSameTraceIdAsTheRequestIdHeader() {
		io.restassured.response.Response response = support.request(school).get("/api/v1/students/not-a-uuid");

		String requestId = response.getHeader("X-Request-Id");
		response.then().body("error.traceId", equalTo(requestId));
	}
}
