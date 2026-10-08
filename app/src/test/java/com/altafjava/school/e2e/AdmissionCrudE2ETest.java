package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.security.PasswordEncoder;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserStatus;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdmissionCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Long tenantId;
	private String adminEmail;
	private String adminPassword;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		adminEmail = "admin-" + suffix + "@school.test";
		adminPassword = "Password123!";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Admission E2E School", "adm-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String submitBody(String applicantFirstName) {
		return "{\"applicantFirstName\":\"" + applicantFirstName + "\",\"applicantLastName\":\"Smith\","
				+ "\"applicantDateOfBirth\":\"2015-01-01\",\"guardianFirstName\":\"Bob\",\"guardianLastName\":"
				+ "\"Smith\",\"guardianEmail\":\"bob-" + UUID.randomUUID().toString().substring(0, 6)
				+ "@family.test\",\"guardianPhone\":\"+14155552671\",\"appliedGrade\":\"Grade 3\"}";
	}

	@Test
	void submitAdmission_asTenantAdmin_returns201() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Alice"))
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.status", equalTo("SUBMITTED"));
	}

	@Test
	void listAdmissions_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void submitAdmission_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Carol"))
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void decideApprove_withoutStudentCode_returns400() {
		String accessToken = login();
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Dave"))
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"outcome\":\"APPROVED\",\"decidedBy\":\"admin\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.patch("/api/v1/admissions/" + publicId + "/decision")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	// Approving submits to the seeded ADMISSION_DECISION workflow and returns 202 — ENROLLED only
	// once that request is separately approved.
	@Test
	void decideApprove_withStudentCode_submitsForApprovalAndEnrollsOnceApproved() {
		String accessToken = login();
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Erin"))
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
		String studentCode = "STU-E2E-" + UUID.randomUUID().toString().substring(0, 6);

		String approvalRequestId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"outcome\":\"APPROVED\",\"decidedBy\":\"admin\",\"studentCode\":\"" + studentCode + "\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.patch("/api/v1/admissions/" + publicId + "/decision")
				.then()
				.statusCode(HttpStatus.ACCEPTED.value())
				.extract().path("data");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/admissions/" + publicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("SUBMITTED"));

		Long principalUserId = createUserWithRole("principal-" + UUID.randomUUID().toString().substring(0, 8)
				+ "@school.test", "PRINCIPAL");
		String principalToken = authHelper.tokenForUser(tenantId, principalUserId, "principal@school.test",
				"PRINCIPAL");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + principalToken)
				.contentType(ContentType.JSON)
				.body("{\"remarks\":\"Approved\"}")
				.when()
				.post("/api/v1/approval/requests/" + approvalRequestId + "/approve")
				.then()
				.statusCode(HttpStatus.OK.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/admissions/" + publicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("ENROLLED"));
	}

	private Long createUserWithRole(String email, String roleName) {
		return withTenant(() -> {
			var role = roleRepository.findAll().stream()
					.filter(r -> roleName.equals(r.getName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Role not seeded for tenant: " + roleName));
			User user = User.builder()
					.email(email)
					.passwordHash(passwordEncoder.encode("Password123!"))
					.status(UserStatus.ACTIVE)
					.emailVerified(true)
					.build();
			user.addRole(role);
			return userRepository.save(user).getId();
		});
	}

	private <T> T withTenant(java.util.function.Supplier<T> action) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return action.get();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String applyBody(String applicantFirstName, String appliedGrade) {
		return "{\"applicantFirstName\":\"" + applicantFirstName + "\",\"applicantLastName\":\"Public\","
				+ "\"applicantDateOfBirth\":\"2015-01-01\",\"guardianFirstName\":\"Gale\",\"guardianLastName\":"
				+ "\"Public\",\"guardianEmail\":\"gale-" + UUID.randomUUID().toString().substring(0, 6)
				+ "@family.test\",\"guardianPhone\":\"555-9999\",\"appliedGrade\":\"" + appliedGrade + "\"}";
	}

	/**
	 * {@code POST /apply} is designed to be permitAll (see {@code AdmissionController#apply}'s
	 * Javadoc), but the matching platform-side {@code SecurityConfig} permitAll entry has not
	 * landed yet (out of scope for this repo) — so today it still falls under the platform's
	 * blanket {@code anyRequest().authenticated()} rule and 401s for an anonymous caller. This
	 * test documents that real, current gap; once the platform-side change lands, replace it with
	 * an assertion that an anonymous call returns 201.
	 */
	@Test
	void applyForAdmission_asAnonymous_returns201() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body(applyBody("Public-Anonymous", "Grade 3"))
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/admissions/apply")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.status", equalTo("SUBMITTED"));
	}

	@Test
	void applyForAdmission_asAnyAuthenticatedRole_returns201() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body(applyBody("Public-Teacher", "Grade 3"))
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/admissions/apply")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.status", equalTo("SUBMITTED"));
	}

	private String submitAndMoveUnderReview(String accessToken, String applicantFirstName, String appliedGrade) {
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"applicantFirstName\":\"" + applicantFirstName + "\",\"applicantLastName\":\"Smith\","
						+ "\"applicantDateOfBirth\":\"2015-01-01\",\"guardianFirstName\":\"Bob\","
						+ "\"guardianLastName\":\"Smith\",\"guardianEmail\":\"bob-"
						+ UUID.randomUUID().toString().substring(0, 6) + "@family.test\",\"guardianPhone\":"
						+ "\"555-1234\",\"appliedGrade\":\"" + appliedGrade + "\"}")
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.when()
				.patch("/api/v1/admissions/" + publicId + "/under-review")
				.then()
				.statusCode(HttpStatus.OK.value());

		return publicId;
	}

	@Test
	void recordEntranceTestScore_asTenantAdmin_returns200WithScore() {
		String accessToken = login();
		String publicId = submitAndMoveUnderReview(accessToken, "Score-Happy", "Grade 4");

		var response = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"score\":88.0,\"maxScore\":100.0}")
				.when()
				.post("/api/v1/admissions/" + publicId + "/entrance-test-score")
				.then()
				.statusCode(HttpStatus.OK.value())
				.extract();

		// Groovy's JSON parser types decimal literals as Float, not Double — closeTo() requires an
		// exact Double, so extract via getDouble() (which coerces) rather than asserting in the DSL chain.
		assertEquals(88.0, response.jsonPath().getDouble("data.entranceTestScore"), 0.01);
		assertEquals(100.0, response.jsonPath().getDouble("data.entranceTestMaxScore"), 0.01);
	}

	@Test
	void recordEntranceTestScore_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("{\"score\":88,\"maxScore\":100}")
				.when()
				.post("/api/v1/admissions/" + UUID.randomUUID() + "/entrance-test-score")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void recordEntranceTestScore_asTeacherRole_returns403() {
		String accessToken = login();
		String publicId = submitAndMoveUnderReview(accessToken, "Score-Forbidden", "Grade 4");
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"score\":88,\"maxScore\":100}")
				.when()
				.post("/api/v1/admissions/" + publicId + "/entrance-test-score")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void generateMeritList_asTenantAdmin_ranksScoredApplicantsAndWaitlistsOverflow() {
		String accessToken = login();
		String grade = "Grade-Merit-" + UUID.randomUUID().toString().substring(0, 6);
		String firstPlaceId = submitAndMoveUnderReview(accessToken, "First", grade);
		String secondPlaceId = submitAndMoveUnderReview(accessToken, "Second", grade);

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"score\":95,\"maxScore\":100}")
				.when()
				.post("/api/v1/admissions/" + firstPlaceId + "/entrance-test-score")
				.then()
				.statusCode(HttpStatus.OK.value());
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"score\":70,\"maxScore\":100}")
				.when()
				.post("/api/v1/admissions/" + secondPlaceId + "/entrance-test-score")
				.then()
				.statusCode(HttpStatus.OK.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.post("/api/v1/admissions/merit-list?appliedGrade=" + grade + "&availableSeats=1")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.size()", equalTo(2));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/admissions/" + firstPlaceId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.meritRank", equalTo(1))
				.body("data.status", equalTo("UNDER_REVIEW"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/admissions/" + secondPlaceId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.meritRank", equalTo(2))
				.body("data.status", equalTo("WAITLISTED"));
	}

	@Test
	void generateMeritList_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.when()
				.post("/api/v1/admissions/merit-list?appliedGrade=Grade+3&availableSeats=1")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void promoteFromWaitlist_asTenantAdmin_returns200WithUnderReviewStatus() {
		String accessToken = login();
		String grade = "Grade-Promote-" + UUID.randomUUID().toString().substring(0, 6);
		String publicId = submitAndMoveUnderReview(accessToken, "Waitlisted", grade);
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"score\":50,\"maxScore\":100}")
				.when()
				.post("/api/v1/admissions/" + publicId + "/entrance-test-score")
				.then()
				.statusCode(HttpStatus.OK.value());
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.post("/api/v1/admissions/merit-list?appliedGrade=" + grade + "&availableSeats=0")
				.then()
				.statusCode(HttpStatus.OK.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.patch("/api/v1/admissions/" + publicId + "/promote-from-waitlist")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("UNDER_REVIEW"));
	}

	@Test
	void promoteFromWaitlist_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.when()
				.patch("/api/v1/admissions/" + UUID.randomUUID() + "/promote-from-waitlist")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void recordEntranceTestScore_forAdmissionInAnotherTenant_returns404() {
		String accessToken = login();
		String publicId = submitAndMoveUnderReview(accessToken, "Isolated", "Grade 6");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other Score School", "adm-score-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.body("{\"score\":88,\"maxScore\":100}")
				.when()
				.post("/api/v1/admissions/" + publicId + "/entrance-test-score")
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void admissionCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Frank"))
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "adm-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/admissions/" + publicId)
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	private String login() {
		return login(tenantId, adminEmail, adminPassword);
	}

	private String login(Long forTenantId, String email, String password) {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given()
					.header("X-Tenant-ID", forTenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
					.when()
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
				break;
			}
		}
		throw new IllegalStateException("login timed out");
	}
}
