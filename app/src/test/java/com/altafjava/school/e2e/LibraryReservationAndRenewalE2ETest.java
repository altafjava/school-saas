package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.school.application.service.BookReservationService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LibraryReservationAndRenewalE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private BookReservationService bookReservationService;

	private School school;
	private String bookPublicId;
	private String copyPublicId;
	private String studentA;
	private String studentB;
	private String studentC;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Library School");
		bookPublicId = support.request(school)
				.body("{\"title\":\"Dune\",\"author\":\"Herbert\"}")
				.post("/api/v1/books")
				.then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		copyPublicId = support.request(school).body("{\"copyCode\":\"DUNE-1\"}")
				.post("/api/v1/books/" + bookPublicId + "/copies")
				.then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		studentA = support.createStudent(school);
		studentB = support.createStudent(school);
		studentC = support.createStudent(school);
	}

	private RequestSpecification mutating() {
		return support.request(school).header("Idempotency-Key", UUID.randomUUID().toString());
	}

	private String checkout(String studentPublicId) {
		return mutating().body("{\"bookCopyPublicId\":\"" + copyPublicId + "\",\"studentPublicId\":\""
				+ studentPublicId + "\"}")
				.post("/api/v1/circulations/checkout")
				.then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
	}

	private String reserve(String studentPublicId) {
		return support.request(school)
				.body("{\"bookPublicId\":\"" + bookPublicId + "\",\"studentPublicId\":\"" + studentPublicId + "\"}")
				.post("/api/v1/book-reservations")
				.then().statusCode(HttpStatus.CREATED.value()).body("data.status", equalTo("QUEUED"))
				.extract().path("data.publicId");
	}

	private void returnLoan(String circulationPublicId) {
		mutating().body("{\"returnedAt\":\"" + LocalDate.now() + "\"}")
				.patch("/api/v1/circulations/" + circulationPublicId + "/return")
				.then().statusCode(HttpStatus.OK.value());
	}

	@Test
	void reservation_queueHoldsTheReturnedCopyForTheFirstInLine() {
		String loanA = checkout(studentA);
		String reservationB = reserve(studentB);
		String reservationC = reserve(studentC);

		support.request(school)
				.body("{\"bookPublicId\":\"" + bookPublicId + "\",\"studentPublicId\":\"" + studentB + "\"}")
				.post("/api/v1/book-reservations")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
		mutating().patch("/api/v1/circulations/" + loanA + "/renew")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());

		returnLoan(loanA);
		support.request(school).get("/api/v1/book-reservations?bookPublicId=" + bookPublicId)
				.then().statusCode(HttpStatus.OK.value())
				.body("data.content.status", contains("READY", "QUEUED"))
				.body("data.content[0].publicId", equalTo(reservationB))
				.body("data.content[1].publicId", equalTo(reservationC));
		support.request(school).get("/api/v1/books/" + bookPublicId + "/copies")
				.then().statusCode(HttpStatus.OK.value()).body("data[0].status", equalTo("ON_HOLD"));

		mutating().body("{\"bookCopyPublicId\":\"" + copyPublicId + "\",\"studentPublicId\":\"" + studentC + "\"}")
				.post("/api/v1/circulations/checkout")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
		String loanB = checkout(studentB);

		support.request(school).get("/api/v1/book-reservations?bookPublicId=" + bookPublicId)
				.then().body("data.content.status", contains("FULFILLED", "QUEUED"));
		mutating().patch("/api/v1/circulations/" + loanB + "/renew")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());

		support.request(school).patch("/api/v1/book-reservations/" + reservationC + "/cancel")
				.then().statusCode(HttpStatus.OK.value()).body("data.status", equalTo("CANCELLED"));
		mutating().patch("/api/v1/circulations/" + loanB + "/renew")
				.then().statusCode(HttpStatus.OK.value()).body("data.renewalCount", equalTo(1));
	}

	@Test
	void cancellingAHeldReservation_passesTheCopyToTheNextMember() {
		String loanA = checkout(studentA);
		String reservationB = reserve(studentB);
		reserve(studentC);
		returnLoan(loanA);

		support.request(school).patch("/api/v1/book-reservations/" + reservationB + "/cancel")
				.then().statusCode(HttpStatus.OK.value());

		support.request(school).get("/api/v1/book-reservations?bookPublicId=" + bookPublicId)
				.then().body("data.content.status", contains("CANCELLED", "READY"));
		support.request(school).get("/api/v1/books/" + bookPublicId + "/copies")
				.then().body("data[0].status", equalTo("ON_HOLD"));
	}

	@Test
	void lapsedHold_isReleasedAndOfferedToTheNextMember() {
		String loanA = checkout(studentA);
		reserve(studentB);
		reserve(studentC);
		returnLoan(loanA);

		int expired = support.inTenant(school.tenantId(),
				() -> bookReservationService.expireLapsedHolds(school.tenantId(), LocalDate.now().plusDays(10)));

		assertEquals(1, expired);
		support.request(school).get("/api/v1/book-reservations?bookPublicId=" + bookPublicId)
				.then().body("data.content.status", contains("EXPIRED", "READY"));
	}

	@Test
	void reserve_whileACopyIsOnTheShelf_returns400() {
		support.request(school)
				.body("{\"bookPublicId\":\"" + bookPublicId + "\",\"studentPublicId\":\"" + studentA + "\"}")
				.post("/api/v1/book-reservations")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void renew_withinTheLimit_extendsTheLoanAndTheLimitThenApplies() {
		String loan = checkout(studentA);

		mutating().patch("/api/v1/circulations/" + loan + "/renew")
				.then().statusCode(HttpStatus.OK.value()).body("data.renewalCount", equalTo(1));
		mutating().patch("/api/v1/circulations/" + loan + "/renew")
				.then().statusCode(HttpStatus.OK.value()).body("data.renewalCount", equalTo(2));
		mutating().patch("/api/v1/circulations/" + loan + "/renew")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void renew_withoutIdempotencyKey_returns400() {
		String loan = checkout(studentA);

		support.request(school).patch("/api/v1/circulations/" + loan + "/renew")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void reserve_asStudentRole_returns403() {
		support.requestWithRole(school.tenantId(), "STUDENT")
				.body("{\"bookPublicId\":\"" + bookPublicId + "\",\"studentPublicId\":\"" + studentA + "\"}")
				.post("/api/v1/book-reservations")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void reservation_isNotVisibleToAnotherTenant() {
		checkout(studentA);
		String reservationPublicId = reserve(studentB);
		School other = support.provisionSchool("Other Library School");

		support.request(other).patch("/api/v1/book-reservations/" + reservationPublicId + "/cancel")
				.then().statusCode(HttpStatus.NOT_FOUND.value());
	}
}
