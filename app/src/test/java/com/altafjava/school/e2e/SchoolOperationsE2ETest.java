package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Year-end promotion, student leave and fee recording, each proved against the real register and balance. */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchoolOperationsE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private TestPublicIds publicIds;
	@Autowired
	private AcademicYearService academicYearService;
	@Autowired
	private ClassroomRepository classroomRepository;
	@Autowired
	private FeePaymentRepository feePaymentRepository;

	private School school;
	private String currentYear;
	private String nextYear;
	private final Map<String, String> yearOfClassroom = new ConcurrentHashMap<>();

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Operations School");
		currentYear = support.currentAcademicYearPublicId(school);
		nextYear = support.inTenant(school.tenantId(), () -> academicYearService
				.create("next-" + suffix(), LocalDate.now().plusYears(1), LocalDate.now().plusYears(2), false)
				.getPublicId().toString());
	}

	@BeforeEach
	void pointRestAssuredAtThisServer() {
		RestAssured.port = port;
		RestAssured.basePath = "";
	}

	// ---- promotion ----

	@Test
	void promotion_movesTheClassUpAndKeepsItsHistory() {
		String source = classroomIn(currentYear);
		String target = classroomIn(nextYear);
		String mover = enrolledStudent(source);
		String repeater = enrolledStudent(source);
		String leaver = enrolledStudent(source);
		String repeatRoom = classroomIn(nextYear);
		admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body(markBody(mover, source, LocalDate.of(2026, 2, 2), "PRESENT")).post("/api/v1/attendance").then()
				.statusCode(HttpStatus.CREATED.value());

		admin().get("/api/v1/classrooms/" + source + "/promotions/preview?targetClassroomPublicId=" + target).then()
				.statusCode(HttpStatus.OK.value())
				.body("data.students", hasSize(3))
				.body("data.students[0].blockedReason", nullValue());

		String body = """
				{"targetClassroomPublicId":"%s","entries":[
				{"studentPublicId":"%s","outcome":"PROMOTE","reason":"Passed"},
				{"studentPublicId":"%s","outcome":"RETAIN","targetClassroomPublicId":"%s"},
				{"studentPublicId":"%s","outcome":"GRADUATE"}]}
				""".formatted(target, mover, repeater, repeatRoom, leaver);
		admin().header("Idempotency-Key", UUID.randomUUID().toString()).body(body)
				.post("/api/v1/classrooms/" + source + "/promotions").then().statusCode(HttpStatus.OK.value())
				.body("data", hasSize(3));

		admin().get("/api/v1/classrooms/" + target + "/students").then().statusCode(HttpStatus.OK.value())
				.body("data.content.publicId", hasItem(mover));
		admin().get("/api/v1/classrooms/" + repeatRoom + "/students").then()
				.body("data.content.publicId", hasItem(repeater));
		admin().get("/api/v1/classrooms/" + source + "/students").then()
				.body("data.content", hasSize(3));
		admin().get("/api/v1/attendance?studentPublicId=" + mover + "&classroomPublicId=" + source).then()
				.body("data.totalElements", equalTo(1));
		admin().get("/api/v1/students/" + mover + "/lifecycle").then()
				.body("data.toStage", hasItem("PROMOTED"));
		admin().get("/api/v1/students/" + repeater + "/lifecycle").then()
				.body("data.toStage", hasItem("RETAINED"));
		admin().get("/api/v1/students/" + leaver).then().body("data.enrollmentStatus", equalTo("GRADUATED"));
	}

	@Test
	void promotion_isAllOrNothing() {
		String source = classroomIn(currentYear);
		String target = classroomIn(nextYear);
		String mover = enrolledStudent(source);
		String outsider = enrolledStudent(classroomIn(currentYear));

		String body = """
				{"targetClassroomPublicId":"%s","entries":[
				{"studentPublicId":"%s","outcome":"PROMOTE"},
				{"studentPublicId":"%s","outcome":"PROMOTE"}]}
				""".formatted(target, mover, outsider);
		admin().header("Idempotency-Key", UUID.randomUUID().toString()).body(body)
				.post("/api/v1/classrooms/" + source + "/promotions").then()
				.statusCode(HttpStatus.BAD_REQUEST.value());

		admin().get("/api/v1/classrooms/" + target + "/students").then().body("data.content", hasSize(0));
	}

	@Test
	void promotion_toTheSameYear_isRefused() {
		String source = classroomIn(currentYear);
		admin().get("/api/v1/classrooms/" + source + "/promotions/preview?targetClassroomPublicId="
				+ classroomIn(currentYear)).then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void retainedStudent_needsTheirOwnClassroom() {
		String source = classroomIn(currentYear);
		String student = enrolledStudent(source);
		String body = """
				{"targetClassroomPublicId":"%s","entries":[{"studentPublicId":"%s","outcome":"RETAIN"}]}
				""".formatted(classroomIn(nextYear), student);
		admin().header("Idempotency-Key", UUID.randomUUID().toString()).body(body)
				.post("/api/v1/classrooms/" + source + "/promotions").then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void promotion_needsThePermission() {
		String source = classroomIn(currentYear);
		support.requestWithRole(school.tenantId(), "TEACHER")
				.get("/api/v1/classrooms/" + source + "/promotions/preview?targetClassroomPublicId=" + source).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		given().header("X-Tenant-ID", school.tenantId())
				.get("/api/v1/classrooms/" + source + "/promotions/preview?targetClassroomPublicId=" + source).then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	// ---- student leave ----

	@Test
	void approvedLeave_showsAsExcusedOnTheRegister_andTheParentSeesIt() {
		Family family = family();
		LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
		LocalDate tuesday = monday.plusDays(1);
		admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body(markBody(family.student(), family.classroom(), monday, "ABSENT")).post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.CREATED.value());
		String leave = submitLeave(family, monday, tuesday).then().statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("PENDING")).extract().path("data.publicId");

		family.classTeacher().patch("/api/v1/student-leave-requests/" + leave + "/approve").then()
				.statusCode(HttpStatus.OK.value()).body("data.status", equalTo("APPROVED"));

		admin().get("/api/v1/attendance?studentPublicId=" + family.student() + "&from=" + monday + "&to=" + tuesday)
				.then().statusCode(HttpStatus.OK.value())
				.body("data.content.status", org.hamcrest.Matchers.everyItem(equalTo("EXCUSED")))
				.body("data.content", hasSize(2));
		family.parent().get("/api/v1/student-leave-requests?studentPublicId=" + family.student()).then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.status", hasItem("APPROVED"));
	}

	@Test
	void approval_keepsAPresentMarkAndSkipsSunday() {
		Family family = family();
		LocalDate saturday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).plusWeeks(1);
		LocalDate sunday = saturday.plusDays(1);
		admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body(markBody(family.student(), family.classroom(), saturday, "PRESENT")).post("/api/v1/attendance")
				.then().statusCode(HttpStatus.CREATED.value());
		String leave = submitLeave(family, saturday, sunday).then().extract().path("data.publicId");

		family.classTeacher().patch("/api/v1/student-leave-requests/" + leave + "/approve").then()
				.statusCode(HttpStatus.OK.value());

		admin().get("/api/v1/attendance?studentPublicId=" + family.student() + "&from=" + saturday + "&to=" + sunday)
				.then().body("data.content", hasSize(1)).body("data.content[0].status", equalTo("PRESENT"));
	}

	@Test
	void overlappingLeave_isRefused() {
		Family family = family();
		LocalDate day = LocalDate.now().plusDays(30);
		submitLeave(family, day, day.plusDays(2)).then().statusCode(HttpStatus.CREATED.value());

		submitLeave(family, day.plusDays(1), day.plusDays(3)).then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void anotherParent_cannotRaiseOrSeeLeaveForThisChild() {
		Family mine = family();
		Family theirs = family();
		LocalDate day = LocalDate.now().plusDays(40);
		String leave = submitLeave(mine, day, day).then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");

		theirs.parent().body(leaveBody(mine.student(), day.plusDays(5), day.plusDays(5)))
				.post("/api/v1/student-leave-requests").then().statusCode(HttpStatus.FORBIDDEN.value());
		theirs.parent().get("/api/v1/student-leave-requests/" + leave).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		theirs.parent().get("/api/v1/student-leave-requests").then().statusCode(HttpStatus.OK.value())
				.body("data.content", hasSize(0));
	}

	@Test
	void onlyTheClassTeacherOrAnAdministrator_decides() {
		Family family = family();
		LocalDate day = LocalDate.now().plusDays(50);
		String leave = submitLeave(family, day, day).then().extract().path("data.publicId");

		family.otherTeacher().patch("/api/v1/student-leave-requests/" + leave + "/approve").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		family.parent().patch("/api/v1/student-leave-requests/" + leave + "/approve").then()
				.statusCode(HttpStatus.FORBIDDEN.value());

		family.classTeacher().body("{\"rejectionReason\":\"Exam week\"}")
				.patch("/api/v1/student-leave-requests/" + leave + "/reject").then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("REJECTED"))
				.body("data.rejectionReason", equalTo("Exam week"));
	}

	@Test
	void requester_canCancelWhilePending() {
		Family family = family();
		LocalDate day = LocalDate.now().plusDays(60);
		String leave = submitLeave(family, day, day).then().extract().path("data.publicId");

		family.parent().patch("/api/v1/student-leave-requests/" + leave + "/cancel").then()
				.statusCode(HttpStatus.OK.value()).body("data.status", equalTo("CANCELLED"));
	}

	@Test
	void studentLeave_isHiddenAcrossTenants() {
		Family family = family();
		LocalDate day = LocalDate.now().plusDays(70);
		String leave = submitLeave(family, day, day).then().extract().path("data.publicId");
		School other = support.provisionSchool("Other Operations School");

		support.request(other).get("/api/v1/student-leave-requests/" + leave).then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	// ---- fee recording ----

	@Test
	void everyMode_reachesTheCollectionReport_andABouncedChequeRestoresTheBalance() {
		String classroom = classroomIn(currentYear);
		String student = enrolledStudent(classroom);
		String fee = feeStructure(classroom, "1000.00");
		LocalDate day = LocalDate.of(2026, 3, 2);

		pay(student, fee, day, "\"paymentMode\":\"CASH\"", "100.00").then().statusCode(HttpStatus.CREATED.value());
		pay(student, fee, day, "\"paymentMode\":\"UPI\",\"transactionReference\":\"UPI-1\"", "200.00").then()
				.statusCode(HttpStatus.CREATED.value());
		String cheque = pay(student, fee, day, "\"paymentMode\":\"CHEQUE\",\"chequeNumber\":\"0045\","
				+ "\"chequeDate\":\"2026-03-01\",\"chequeBank\":\"SBI\"", "300.00").then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.paymentMode", equalTo("CHEQUE"))
				.body("data.chequeBank", equalTo("SBI"))
				.body("data.status", equalTo("CLEARED"))
				.extract().path("data.publicId");
		admin().get("/api/v1/students/" + student + "/fee-balance").then()
				.body("data[0].outstandingBalance", equalTo(400.0f));

		admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"reason\":\"Insufficient funds\",\"bouncedAt\":\"2026-03-05T11:00:00\"}")
				.post("/api/v1/fee-payments/" + cheque + "/bounce").then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("BOUNCED"))
				.body("data.bouncedAt", notNullValue());

		admin().get("/api/v1/students/" + student + "/fee-balance").then()
				.body("data[0].outstandingBalance", equalTo(700.0f));
		admin().get("/api/v1/fee-payments/collection-report?from=2026-03-02&to=2026-03-02").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.find { it.paymentMode == 'CASH' }.clearedAmount.toFloat()", equalTo(100.0f))
				.body("data.find { it.paymentMode == 'UPI' }.clearedAmount.toFloat()", equalTo(200.0f))
				.body("data.find { it.paymentMode == 'CHEQUE' }.clearedAmount.toFloat()", equalTo(0.0f))
				.body("data.find { it.paymentMode == 'CHEQUE' }.bouncedAmount.toFloat()", equalTo(300.0f))
				.body("data.find { it.paymentMode == 'CHEQUE' }.bouncedCount", equalTo(1));
		admin().get("/api/v1/fee-payments?studentPublicId=" + student + "&paymentMode=CHEQUE").then()
				.body("data.content.status", hasItem("BOUNCED"));
	}

	@Test
	void bouncedCheque_leavesEveryCollectedTotal_andCannotBeRefunded() {
		String classroom = classroomIn(currentYear);
		String student = enrolledStudent(classroom);
		String fee = feeStructure(classroom, "1000.00");
		BigDecimal before = collectedSoFar();
		String cheque = payByCheque(student, fee, "250.00");
		assertEquals(0, before.add(new BigDecimal("250.00")).compareTo(collectedSoFar()));

		bounce(cheque).then().statusCode(HttpStatus.OK.value());

		assertEquals(0, before.compareTo(collectedSoFar()));
		refund(cheque).then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void refundedCheque_cannotBounce() {
		String classroom = classroomIn(currentYear);
		String student = enrolledStudent(classroom);
		String fee = feeStructure(classroom, "1000.00");
		String cheque = payByCheque(student, fee, "250.00");
		refund(cheque).then().statusCode(HttpStatus.CREATED.value());

		bounce(cheque).then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void chequeWithoutDetails_isRefused() {
		String classroom = classroomIn(currentYear);
		String student = enrolledStudent(classroom);
		String fee = feeStructure(classroom, "1000.00");

		pay(student, fee, LocalDate.of(2026, 3, 3), "\"paymentMode\":\"CHEQUE\"", "10.00").then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void gatewayMode_cannotBeTypedIn() {
		String classroom = classroomIn(currentYear);
		String student = enrolledStudent(classroom);
		String fee = feeStructure(classroom, "1000.00");

		pay(student, fee, LocalDate.of(2026, 3, 3), "\"paymentMode\":\"GATEWAY\"", "10.00").then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void cashPayment_cannotBounce() {
		String classroom = classroomIn(currentYear);
		String student = enrolledStudent(classroom);
		String fee = feeStructure(classroom, "1000.00");
		String cash = pay(student, fee, LocalDate.of(2026, 3, 3), "\"paymentMode\":\"CASH\"", "10.00").then()
				.extract().path("data.publicId");

		admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"reason\":\"No\",\"bouncedAt\":\"2026-03-05T11:00:00\"}")
				.post("/api/v1/fee-payments/" + cash + "/bounce").then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void collectionReport_needsThePermission_andStaysInsideTheTenant() {
		support.requestWithRole(school.tenantId(), "TEACHER")
				.get("/api/v1/fee-payments/collection-report?from=2026-03-02&to=2026-03-02").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		School other = support.provisionSchool("Empty School");
		support.request(other).get("/api/v1/fee-payments/collection-report?from=2026-03-02&to=2026-03-02").then()
				.statusCode(HttpStatus.OK.value()).body("data", hasSize(0));
	}

	@Test
	void collectionReport_rangeIsBounded() {
		admin().get("/api/v1/fee-payments/collection-report?from=2026-01-01&to=2026-12-31").then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	// ---- fixtures ----

	private record Family(String student, String classroom, RequestSpecification parentSpec,
			RequestSpecification teacherSpec, RequestSpecification otherTeacherSpec) {

		RequestSpecification parent() {
			return given().spec(parentSpec);
		}

		RequestSpecification classTeacher() {
			return given().spec(teacherSpec);
		}

		RequestSpecification otherTeacher() {
			return given().spec(otherTeacherSpec);
		}
	}

	private Family family() {
		String classroom = classroomIn(currentYear);
		String student = enrolledStudent(classroom);
		Long parentUser = support.createUserWithRole(school.tenantId(), email("parent"), "PARENT");
		String guardian = admin()
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"" + email("guardian")
						+ "\",\"phone\":\"+14155552671\",\"userPublicId\":\"" + publicIds.of(User.class, parentUser)
						+ "\"}")
				.post("/api/v1/guardians").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		admin().body(
				"{\"studentPublicId\":\"" + student + "\",\"relationshipType\":\"MOTHER\",\"primaryContact\":true}")
				.post("/api/v1/guardians/" + guardian + "/students").then().statusCode(HttpStatus.CREATED.value());
		Long teacherUser = teacherLeading(classroom);
		Long otherTeacherUser = support.createUserWithRole(school.tenantId(), email("other"), "TEACHER");
		return new Family(student, classroom, support.requestAsUser(school.tenantId(), parentUser, "PARENT"),
				support.requestAsUser(school.tenantId(), teacherUser, "TEACHER"),
				support.requestAsUser(school.tenantId(), otherTeacherUser, "TEACHER"));
	}

	private Long teacherLeading(String classroomPublicId) {
		Long userId = support.createUserWithRole(school.tenantId(), email("teacher"), "TEACHER");
		String teacher = admin()
				.body("{\"firstName\":\"Tara\",\"lastName\":\"Rao\",\"email\":\"" + email("t")
						+ "\",\"gender\":\"FEMALE\","
						+ "\"joinDate\":\"2024-06-01\"}")
				.post("/api/v1/teachers").then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		support.linkEmployeeToUser(school, teacher, userId);
		Long version = support.inTenant(school.tenantId(), () -> classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), school.tenantId()).orElseThrow()
				.getVersion());
		admin().body("{\"teacherPublicId\":\"" + teacher + "\",\"version\":" + version + "}")
				.patch("/api/v1/classrooms/" + classroomPublicId + "/teacher").then().statusCode(HttpStatus.OK.value());
		return userId;
	}

	private Response submitLeave(Family family, LocalDate start, LocalDate end) {
		return family.parent().body(leaveBody(family.student(), start, end)).post("/api/v1/student-leave-requests");
	}

	private static String leaveBody(String student, LocalDate start, LocalDate end) {
		return "{\"studentPublicId\":\"" + student + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
				+ "\",\"reason\":\"Fever\"}";
	}

	private static String markBody(String student, String classroom, LocalDate date, String status) {
		return "{\"studentPublicId\":\"" + student + "\",\"classroomPublicId\":\"" + classroom
				+ "\",\"attendanceDate\":\"" + date + "\",\"status\":\"" + status + "\"}";
	}

	private String classroomIn(String academicYearPublicId) {
		String code = "C" + suffix();
		String classroom = admin()
				.body("{\"classCode\":\"" + code + "\",\"grade\":\"Grade 5\",\"section\":\"A\","
						+ "\"academicYearPublicId\":\"" + academicYearPublicId + "\"}")
				.post("/api/v1/classrooms").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		yearOfClassroom.put(classroom, academicYearPublicId);
		return classroom;
	}

	private String enrolledStudent(String classroom) {
		String student = support.createStudent(school);
		String year = yearOfClassroom.get(classroom);
		admin().body("{\"studentPublicId\":\"" + student + "\",\"academicYearPublicId\":\"" + year + "\"}")
				.post("/api/v1/classrooms/" + classroom + "/students").then().statusCode(HttpStatus.CREATED.value());
		return student;
	}

	private String feeStructure(String classroom, String amount) {
		String fee = admin()
				.body("{\"name\":\"Tuition " + suffix() + "\",\"amount\":" + amount
						+ ",\"frequency\":\"ANNUAL\",\"planType\":\"Standard\"}")
				.post("/api/v1/fee-structures").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		admin().body("{\"classroomPublicId\":\"" + classroom + "\"}")
				.post("/api/v1/fee-structures/" + fee + "/assignments").then().statusCode(HttpStatus.CREATED.value());
		return fee;
	}

	private Response pay(String student, String fee, LocalDate day, String modeFields, String amount) {
		return admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"studentPublicId\":\"" + student + "\",\"feeStructurePublicId\":\"" + fee
						+ "\",\"paidAmount\":"
						+ amount + ",\"paidAt\":\"" + day + "T10:00:00\"," + modeFields + "}")
				.post("/api/v1/fee-payments");
	}

	private String payByCheque(String student, String fee, String amount) {
		return pay(student, fee, LocalDate.of(2026, 3, 4), "\"paymentMode\":\"CHEQUE\",\"chequeNumber\":\"0046\","
				+ "\"chequeDate\":\"2026-03-01\",\"chequeBank\":\"SBI\"", amount).then()
				.statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
	}

	private Response bounce(String payment) {
		return admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"reason\":\"Insufficient funds\",\"bouncedAt\":\"2026-03-05T11:00:00\"}")
				.post("/api/v1/fee-payments/" + payment + "/bounce");
	}

	private Response refund(String payment) {
		return admin().header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"amount\":10.00,\"reason\":\"Overpaid\"}")
				.post("/api/v1/fee-payments/" + payment + "/refunds");
	}

	private BigDecimal collectedSoFar() {
		return support.inTenant(school.tenantId(),
				() -> feePaymentRepository.sumClearedAmountByTenantId(school.tenantId()));
	}

	private RequestSpecification admin() {
		return support.request(school).contentType(ContentType.JSON);
	}

	private static String suffix() {
		return UUID.randomUUID().toString().substring(0, 6);
	}

	private static String email(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@school.test";
	}
}
