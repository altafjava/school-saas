package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeInstallment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeAssignmentRepository;
import com.altafjava.school.domain.fee.repository.FeeInstallmentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Invoices over one student with tuition due this month and a bus fee in two monthly installments,
 * a classmate who owes nothing, and the student's parent.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeeInvoiceE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private TestPublicIds publicIds;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private FeeStructureRepository feeStructureRepository;
	@Autowired
	private FeeAssignmentRepository feeAssignmentRepository;
	@Autowired
	private FeeInstallmentRepository feeInstallmentRepository;

	private School school;
	private String student;
	private String classmate;
	private String classroom;
	private String tuition;
	private String bus;
	private Long parentUser;
	private LocalDate monthStart;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Invoice School");
		monthStart = LocalDate.now().withDayOfMonth(1);
		String year = support.currentAcademicYearPublicId(school);
		classroom = admin().body("{\"classCode\":\"INV-" + suffix() + "\",\"grade\":\"6\",\"section\":\"B\","
				+ "\"academicYearPublicId\":\"" + year + "\"}").post("/api/v1/classrooms").then()
				.statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		student = support.createStudent(school);
		classmate = support.createStudent(school);
		for (String enrolled : new String[] { student, classmate }) {
			admin().body("{\"studentPublicId\":\"" + enrolled + "\",\"academicYearPublicId\":\"" + year + "\"}")
					.post("/api/v1/classrooms/" + classroom + "/students").then()
					.statusCode(HttpStatus.CREATED.value());
		}
		Long studentId = support.inTenant(school.tenantId(), () -> studentRepository
				.findByPublicIdAndTenantId(UUID.fromString(student), school.tenantId()).orElseThrow().getId());
		support.inTenant(school.tenantId(), () -> {
			FeeStructure tuitionFee = feeStructureRepository
					.save(FeeStructure.create("Tuition", new BigDecimal("1200"), FeeFrequency.ANNUAL, null));
			FeeAssignment tuitionAssignment = FeeAssignment.forStudent(tuitionFee.getId(), studentId);
			tuitionAssignment.configureDueDate(monthStart.plusDays(9), null, null);
			feeAssignmentRepository.save(tuitionAssignment);
			FeeStructure busFee = feeStructureRepository
					.save(FeeStructure.create("Bus", new BigDecimal("600"), FeeFrequency.ANNUAL, null));
			feeAssignmentRepository.save(FeeAssignment.forStudent(busFee.getId(), studentId));
			feeInstallmentRepository
					.save(FeeInstallment.of(studentId, busFee.getId(), 1, monthStart.plusDays(4), 5000));
			feeInstallmentRepository
					.save(FeeInstallment.of(studentId, busFee.getId(), 2, monthStart.plusMonths(1).plusDays(4), 5000));
			tuition = publicIds.of(FeeStructure.class, tuitionFee.getId());
			bus = publicIds.of(FeeStructure.class, busFee.getId());
		});
		parentUser = support.createUserWithRole(school.tenantId(), "parent-" + suffix() + "@school.test", "PARENT");
		String guardian = admin().body("{\"firstName\":\"Pia\",\"lastName\":\"Doe\",\"email\":\"pia-" + suffix()
				+ "@school.test\",\"phone\":\"+14155552671\",\"userPublicId\":\"" + publicIds.of(User.class, parentUser)
				+ "\"}").post("/api/v1/guardians").then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		admin().body(
				"{\"studentPublicId\":\"" + student + "\",\"relationshipType\":\"MOTHER\",\"primaryContact\":true}")
				.post("/api/v1/guardians/" + guardian + "/students").then().statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void invoicing_billsEachChargeOnce_andTracksItsPayments() {
		String november = issue(student, monthStart).then().statusCode(HttpStatus.CREATED.value())
				.body("data.invoiceNumber", startsWith("INV-"))
				.body("data.lines.description", containsInAnyOrder("Tuition", "Bus — installment 1"))
				.body("data.totalAmount", equalTo(1500.0F))
				.body("data.settlement", equalTo("UNPAID"))
				.extract().path("data.publicId");
		issue(student, monthStart).then().statusCode(HttpStatus.CONFLICT.value());

		String december = issue(student, monthStart.plusMonths(1)).then().statusCode(HttpStatus.CREATED.value())
				.body("data.lines.description", contains("Bus — installment 2"))
				.extract().path("data.publicId");

		pay(tuition, "1200", november).then().statusCode(HttpStatus.CREATED.value())
				.body("data.invoicePublicId", equalTo(november));
		pay(tuition, "100", december).then().statusCode(HttpStatus.BAD_REQUEST.value());
		admin().get("/api/v1/fee-invoices/" + november).then()
				.body("data.paidAmount", equalTo(1200.0F))
				.body("data.outstandingAmount", equalTo(300.0F))
				.body("data.settlement", equalTo("PARTIALLY_PAID"));

		admin().body("{\"reason\":\"Issued in error\"}").post("/api/v1/fee-invoices/" + november + "/cancel").then()
				.statusCode(HttpStatus.CONFLICT.value());
		admin().body("{\"reason\":\"Issued in error\"}").post("/api/v1/fee-invoices/" + december + "/cancel").then()
				.statusCode(HttpStatus.OK.value()).body("data.settlement", equalTo("CANCELLED"));
		issue(student, monthStart.plusMonths(1)).then().statusCode(HttpStatus.CREATED.value())
				.body("data.lines.description", contains("Bus — installment 2"));
	}

	@Test
	void invoice_isAPdf() {
		String invoice = invoiceFor(monthStart.plusMonths(6));

		Response pdf = admin().get("/api/v1/fee-invoices/" + invoice + "/pdf");
		pdf.then().statusCode(HttpStatus.OK.value()).contentType("application/pdf");
		org.assertj.core.api.Assertions.assertThat(new String(pdf.asByteArray(), 0, 4)).isEqualTo("%PDF");
	}

	@Test
	void classroomRun_invoicesWhoeverOwes_andReportsTheRest() {
		admin().body("{\"classroomPublicId\":\"" + classroom + "\",\"periodStart\":\"" + monthStart.plusMonths(3)
				+ "\",\"periodEnd\":\"" + monthStart.plusMonths(4).minusDays(1) + "\",\"dueDate\":\""
				+ monthStart.plusMonths(3).plusDays(10) + "\"}").post("/api/v1/fee-invoices/classroom-runs").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.skipped.studentPublicId", hasItem(classmate))
				.body("data.skipped.find { it.studentPublicId == '" + classmate + "' }.reason",
						equalTo("Nothing due in the period"));
	}

	@Test
	void parent_seesTheirChildsInvoicesOnly() {
		invoiceFor(monthStart.plusMonths(8));
		parent().get("/api/v1/students/" + student + "/fee-invoices").then().statusCode(HttpStatus.OK.value())
				.body("data.content.studentPublicId", org.hamcrest.Matchers.everyItem(equalTo(student)));
		parent().get("/api/v1/students/" + classmate + "/fee-invoices").then().statusCode(HttpStatus.FORBIDDEN.value());
		parent().get("/api/v1/fee-invoices").then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void invoices_asATeacher_returns403() {
		support.requestWithRole(school.tenantId(), "TEACHER").get("/api/v1/fee-invoices").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void invoices_withoutLogin_returns401() {
		given().header("X-Tenant-ID", school.tenantId()).get("/api/v1/fee-invoices").then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void anotherSchool_cannotReachThisSchoolsInvoices() {
		String invoice = invoiceFor(monthStart.plusMonths(10));
		School other = support.provisionSchool("Other Invoice School");

		support.request(other).get("/api/v1/fee-invoices/" + invoice).then().statusCode(HttpStatus.NOT_FOUND.value());
		support.request(other).get("/api/v1/fee-invoices").then().body("data.content", hasSize(0));
		support.request(other).body("{\"studentPublicId\":\"" + student + "\",\"periodStart\":\"" + monthStart
				+ "\",\"periodEnd\":\"" + monthStart.plusDays(5) + "\",\"dueDate\":\"" + monthStart.plusDays(20)
				+ "\"}")
				.post("/api/v1/fee-invoices").then().statusCode(HttpStatus.NOT_FOUND.value());
	}

	// Gives the month a charge of its own, so the invoice for it has something to bill.
	private String invoiceFor(LocalDate start) {
		support.inTenant(school.tenantId(), () -> {
			Long studentId = studentRepository.findByPublicIdAndTenantId(UUID.fromString(student), school.tenantId())
					.orElseThrow().getId();
			FeeStructure fee = feeStructureRepository
					.save(FeeStructure.create("Trip " + start, new BigDecimal("50"), FeeFrequency.ONE_TIME, null));
			FeeAssignment assignment = FeeAssignment.forStudent(fee.getId(), studentId);
			assignment.configureDueDate(start.plusDays(2), null, null);
			feeAssignmentRepository.save(assignment);
		});
		return issue(student, start).then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
	}

	private Response issue(String studentPublicId, LocalDate start) {
		LocalDate end = start.plusMonths(1).minusDays(1);
		LocalDate due = start.plusDays(10).isBefore(LocalDate.now()) ? LocalDate.now().plusDays(10)
				: start.plusDays(10);
		return admin().body("{\"studentPublicId\":\"" + studentPublicId + "\",\"periodStart\":\"" + start
				+ "\",\"periodEnd\":\"" + end + "\",\"dueDate\":\"" + due + "\"}").post("/api/v1/fee-invoices");
	}

	private Response pay(String feeStructure, String amount, String invoice) {
		return admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"studentPublicId\":\"" + student + "\",\"feeStructurePublicId\":\"" + feeStructure
						+ "\",\"paidAmount\":" + amount + ",\"paidAt\":\"" + LocalDate.now().atTime(10, 0)
						+ "\",\"paymentMode\":\"CASH\",\"invoicePublicId\":\"" + invoice + "\"}")
				.post("/api/v1/fee-payments");
	}

	private RequestSpecification admin() {
		return support.request(school);
	}

	private RequestSpecification parent() {
		return support.requestAsUser(school.tenantId(), parentUser, "PARENT");
	}

	private static String suffix() {
		return UUID.randomUUID().toString().substring(0, 6);
	}
}
