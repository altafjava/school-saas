package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import java.util.UUID;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * The REST surface of Phase 2: employees, student lifecycle, fee adjustments and the admission
 * application fee — wiring, validation, permissions and JSON shapes.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Phase2ApiE2ETest extends SchoolIntegrationTestBase {

	private static final Matcher<Integer> CLIENT_ERROR = allOf(greaterThanOrEqualTo(400), lessThan(500));

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;
	@Autowired
	private SchoolAuthenticationHelper authHelper;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private FeeStructureRepository feeStructureRepository;

	private Long tenantId;
	private String adminToken;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String email = "admin-" + suffix + "@school.test";
		Tenant tenant = onboardingService.registerTenant(
				new RegisterTenantCommand("Phase2 E2E", "p2-" + suffix, 1L, email, "Password123!", "USD"));
		tenantId = tenant.getId();
		adminToken = login(email);
	}

	private String login(String email) {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given().header("X-Tenant-ID", tenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}")
					.post("/api/v1/auth/login");
			if (response.statusCode() == HttpStatus.OK.value()) {
				return response.then().extract().path("data.accessToken");
			}
			if (System.currentTimeMillis() >= deadline) {
				response.then().statusCode(HttpStatus.OK.value());
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private RequestSpecification asAdmin() {
		return given().header("X-Tenant-ID", tenantId).header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON);
	}

	private RequestSpecification asTeacher() {
		return given().header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + authHelper.tokenWithRole(tenantId, "TEACHER"))
				.contentType(ContentType.JSON);
	}

	private String enrollStudent() {
		String code = "STU-" + UUID.randomUUID().toString().substring(0, 8);
		return asAdmin().body("""
				{"studentCode":"%s","firstName":"Alice","lastName":"Smith","email":"%s@school.test",
				"dateOfBirth":"2010-01-01"}""".formatted(code, code.toLowerCase()))
				.post("/api/v1/students").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
	}

	private Long studentId(String publicId) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return studentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId).orElseThrow()
					.getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private Long structureId(String publicId) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return feeStructureRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
					.orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	// ---- employees ----

	@Test
	void employees_hireListAndLeave() {
		String publicId = asAdmin().body("""
				{"staffCategory":"SUPPORT","firstName":"Sam","lastName":"Driver","email":"sam@school.test",
				"joinDate":"2024-01-01"}""")
				.post("/api/v1/employees").then().statusCode(HttpStatus.CREATED.value())
				.body("data.staffCategory", equalTo("SUPPORT")).body("data.status", equalTo("ACTIVE"))
				.body("data.employeeCode", notNullValue()).extract().path("data.publicId");

		asAdmin().queryParam("category", "SUPPORT").get("/api/v1/employees").then()
				.statusCode(HttpStatus.OK.value()).body("data.content.publicId", hasItem(publicId));

		asAdmin().body("""
				{"status":"RESIGNED","exitDate":"2026-01-15","reason":"Relocating"}""")
				.patch("/api/v1/employees/" + publicId + "/exit").then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("RESIGNED")).body("data.exitDate", equalTo("2026-01-15"));

		asAdmin().body("{\"firstName\":\"S\",\"lastName\":\"D\",\"email\":\"s@school.test\"}")
				.patch("/api/v1/employees/" + publicId + "/contact-details").then().statusCode(CLIENT_ERROR);
	}

	@Test
	void employees_teachingStaffAreHiredAsTeachers_andTeachersAreEmployeesToo() {
		asAdmin().body("""
				{"staffCategory":"TEACHING","firstName":"Jane","lastName":"Doe","email":"jane@school.test",
				"joinDate":"2024-01-01"}""")
				.post("/api/v1/employees").then().statusCode(CLIENT_ERROR);

		String teacherId = asAdmin().body("""
				{"firstName":"Jane","lastName":"Doe","email":"jane2@school.test","joinDate":"2024-01-01"}""")
				.post("/api/v1/teachers").then().statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("ACTIVE")).extract().path("data.publicId");

		asAdmin().body("{\"designation\":\"Head of Science\"}")
				.patch("/api/v1/employees/" + teacherId + "/hr-details").then().statusCode(HttpStatus.OK.value())
				.body("data.designation", equalTo("Head of Science"))
				.body("data.staffCategory", equalTo("TEACHING"));
	}

	@Test
	void employees_requirePermissions() {
		asTeacher().get("/api/v1/employees").then().statusCode(HttpStatus.FORBIDDEN.value());
		given().header("X-Tenant-ID", tenantId).get("/api/v1/employees").then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	// ---- lifecycle ----

	@Test
	void lifecycle_statusChangesBuildATimeline() {
		String student = enrollStudent();

		asAdmin().body("{\"reason\":\"Exam malpractice\"}").patch("/api/v1/students/" + student + "/suspend")
				.then().statusCode(HttpStatus.OK.value()).body("data.enrollmentStatus", equalTo("SUSPENDED"));
		asAdmin().patch("/api/v1/students/" + student + "/reinstate").then().statusCode(HttpStatus.OK.value());
		asAdmin().patch("/api/v1/students/" + student + "/withdraw").then().statusCode(HttpStatus.OK.value())
				.body("data.enrollmentStatus", equalTo("WITHDRAWN"));

		asAdmin().get("/api/v1/students/" + student + "/lifecycle").then().statusCode(HttpStatus.OK.value())
				.body("data.toStage", org.hamcrest.Matchers.contains("ENROLLED", "SUSPENDED", "ENROLLED", "WITHDRAWN"))
				.body("data[1].reason", equalTo("Exam malpractice"));
		asAdmin().patch("/api/v1/students/" + student + "/withdraw").then().statusCode(CLIENT_ERROR);
	}

	@Test
	void lifecycle_aFutureEffectiveDateIsRejected() {
		String student = enrollStudent();

		asAdmin().body("{\"effectiveOn\":\"2999-01-01\"}").patch("/api/v1/students/" + student + "/withdraw")
				.then().statusCode(CLIENT_ERROR);
	}

	// ---- fee adjustments ----

	@Test
	void feeAdjustments_discountPlanAndRefundShowUpInTheBalance() {
		String student = enrollStudent();
		String structure = asAdmin().body("""
				{"name":"Tuition %s","amount":1000.00,"frequency":"ANNUAL","planType":"Standard"}"""
				.formatted(UUID.randomUUID().toString().substring(0, 6)))
				.post("/api/v1/fee-structures").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		asAdmin().body("{\"studentPublicId\":\"" + student + "\"}")
				.post("/api/v1/fee-structures/" + structure + "/assignments").then()
				.statusCode(HttpStatus.CREATED.value());

		asAdmin().body("""
				{"feeStructurePublicId":"%s","type":"PERCENTAGE","value":20,"category":"sibling",
				"reason":"Second child"}""".formatted(structure))
				.post("/api/v1/students/" + student + "/fee-discounts").then().statusCode(HttpStatus.CREATED.value())
				.body("data.category", equalTo("SIBLING")).body("data.active", equalTo(true));

		asAdmin().body(
				"{\"count\":4,\"firstDueDate\":\"2099-01-01\",\"schedule\":[{\"dueDate\":\"2099-01-01\",\"percentage\":100}]}")
				.put("/api/v1/students/" + student + "/fee-installment-plans/" + structure).then()
				.statusCode(CLIENT_ERROR);
		asAdmin().body("{\"count\":4,\"firstDueDate\":\"2099-01-01\"}")
				.put("/api/v1/students/" + student + "/fee-installment-plans/" + structure).then()
				.statusCode(HttpStatus.OK.value()).body("data.size()", equalTo(4))
				.body("data[0].sharePercentage", equalTo(25.0f));

		asAdmin().get("/api/v1/students/" + student + "/fee-balance").then().statusCode(HttpStatus.OK.value())
				.body("data[0].grossAmount", equalTo(1000.0f)).body("data[0].discountAmount", equalTo(200.0f))
				.body("data[0].amountDue", equalTo(800.0f)).body("data[0].installments.size()", equalTo(4))
				.body("data[0].installments[0].amount", equalTo(200.0f));

		String payment = asAdmin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"studentId\":" + studentId(student) + ",\"feeStructureId\":" + structureId(structure)
						+ ",\"paidAmount\":800.00,\"paidAt\":\"2026-02-01T10:00:00\",\"receiptNumber\":\"RCPT-"
						+ UUID.randomUUID().toString().substring(0, 8) + "\"}")
				.post("/api/v1/fee-payments").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");

		asTeacher().body("{\"amount\":100,\"reason\":\"x\"}").post("/api/v1/fee-payments/" + payment + "/refunds")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
		asAdmin().body("{\"amount\":300,\"reason\":\"Billing error\"}")
				.post("/api/v1/fee-payments/" + payment + "/refunds").then().statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("COMPLETED")).body("data.method", equalTo("MANUAL"))
				.body("data.creditNoteNumber", notNullValue());
		asAdmin().body("{\"amount\":600,\"reason\":\"Too much\"}").post("/api/v1/fee-payments/" + payment + "/refunds")
				.then().statusCode(CLIENT_ERROR);

		asAdmin().get("/api/v1/students/" + student + "/fee-balance").then().body("data[0].amountPaid", equalTo(500.0f))
				.body("data[0].refundedAmount", equalTo(300.0f));
	}

	// ---- admissions ----

	private static final String APPLICATION = """
			{"applicantFirstName":"Alice","applicantLastName":"Smith","applicantDateOfBirth":"2015-01-01",
			"guardianFirstName":"Bob","guardianLastName":"Smith","guardianEmail":"bob@family.test",
			"guardianPhone":"+14155552671","appliedGrade":"Grade 3"}""";

	@Test
	void admissions_applicationFeeGatesReview_andOfferLetterNeedsApproval() {
		asAdmin().body("{\"applicationFeeAmount\":300.00}").put("/api/v1/admission-settings").then()
				.statusCode(HttpStatus.OK.value()).body("data.applicationFeeAmount", equalTo(300.0f));

		String admission = given().header("X-Tenant-ID", tenantId).contentType(ContentType.JSON).body(APPLICATION)
				.post("/api/v1/admissions/apply").then().statusCode(HttpStatus.CREATED.value())
				.body("data.applicationFeeStatus", equalTo("PENDING"))
				.body("data.applicationFeeAmount", equalTo(300.0f)).extract().path("data.publicId");

		asAdmin().patch("/api/v1/admissions/" + admission + "/under-review").then().statusCode(CLIENT_ERROR);
		asAdmin().patch("/api/v1/admissions/" + admission + "/application-fee/payment").then()
				.statusCode(HttpStatus.OK.value()).body("data.applicationFeeStatus", equalTo("PAID"))
				.body("data.applicationFeeReceiptNumber", notNullValue());
		asAdmin().patch("/api/v1/admissions/" + admission + "/application-fee/waive")
				.then().statusCode(CLIENT_ERROR);
		asAdmin().patch("/api/v1/admissions/" + admission + "/under-review").then()
				.statusCode(HttpStatus.OK.value());

		asAdmin().get("/api/v1/admissions/" + admission + "/offer-letter/download").then()
				.statusCode(HttpStatus.NOT_FOUND.value());
		asAdmin().post("/api/v1/admissions/" + admission + "/offer-letter").then().statusCode(CLIENT_ERROR);
		asAdmin().get("/api/v1/admissions/" + admission + "/lifecycle").then().statusCode(HttpStatus.OK.value())
				.body("data.toStage", org.hamcrest.Matchers.contains("SUBMITTED", "UNDER_REVIEW"));
	}

	@Test
	void admissions_settingsAndFeeOperationsRequireTheFeePermission() {
		asTeacher().body("{\"applicationFeeAmount\":1}").put("/api/v1/admission-settings").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		asTeacher().patch("/api/v1/admissions/" + UUID.randomUUID() + "/application-fee/payment").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}
}
