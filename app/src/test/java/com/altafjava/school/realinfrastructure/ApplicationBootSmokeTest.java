package com.altafjava.school.realinfrastructure;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.base.SchoolInfrastructureTestBase;
import com.altafjava.school.demo.DemoDataSeeder;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Boots the real application and signs in as every seeded role to read and write through HTTP. */
class ApplicationBootSmokeTest extends SchoolInfrastructureTestBase {

	private static final String ADMIN = DemoDataSeeder.ADMIN_EMAIL;
	private static final String BASE_PATH = "/api/v1";

	private record RoleJourney(String email, String role, String readPath, Method writeMethod, String writePath,
			int writeStatus, Map<String, Object> writeBody) {

		static RoleJourney creating(String email, String role, String readPath, String writePath,
				Map<String, Object> body) {
			return new RoleJourney(email, role, readPath, Method.POST, writePath, 201, body);
		}

		// Principal and Finance hold no create permission of their own beyond approvals, which need
		// a pending record, so their write is the one every signed-in user has.
		static RoleJourney markingNotificationsRead(String email, String role, String readPath) {
			return new RoleJourney(email, role, readPath, Method.PUT, "/notifications/read-all", 200, Map.of());
		}

		@Override
		public String toString() {
			return role;
		}
	}

	@LocalServerPort
	private int port;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private StringRedisTemplate redis;

	private Long tenantId;

	@BeforeEach
	void resolveDemoTenant() {
		tenantId = tenantRepository.findBySubdomain(DemoDataSeeder.SUBDOMAIN).orElseThrow().getId();
		// All logins here share one client IP, so without this the suite trips the real login limit.
		Set<String> loginBuckets = redis.keys("endpoint:login:*");
		redis.delete(loginBuckets);
	}

	static Stream<RoleJourney> roleJourneys() {
		return Stream.of(
				RoleJourney.creating(ADMIN, "TENANT_ADMIN", "/students", "/departments",
						Map.of("code", unique("DEP"), "name", "Smoke department")),
				RoleJourney.markingNotificationsRead("principal@demo.school", SchoolRoles.PRINCIPAL,
						"/dashboards/principal"),
				RoleJourney.markingNotificationsRead("finance@demo.school", SchoolRoles.FINANCE,
						"/dashboards/finance"),
				RoleJourney.creating("hr@demo.school", SchoolRoles.HR, "/dashboards/hr", "/pay-component-definitions",
						Map.of("code", unique("PAY"), "name", "Smoke allowance", "type", "EARNING", "displayOrder", 1)),
				RoleJourney.creating("academic@demo.school", SchoolRoles.ACADEMIC, "/dashboards/academic", "/venues",
						Map.of("code", unique("VEN"), "name", "Smoke hall", "venueType", "HALL")),
				RoleJourney.creating("teacher@demo.school", SchoolRoles.TEACHER, "/classrooms", "/tickets", ticket()),
				RoleJourney.creating("student@demo.school", SchoolRoles.STUDENT, "/me/school-profile", "/tickets",
						ticket()),
				RoleJourney.creating("parent@demo.school", SchoolRoles.PARENT, "/me/school-profile", "/tickets",
						ticket()));
	}

	@ParameterizedTest
	@MethodSource("roleJourneys")
	void role_signsIn_readsAndWrites(RoleJourney journey) {
		String token = signIn(journey.email());

		as(token).get("/me").then().statusCode(200).body("data.roles", hasItem(journey.role()));
		as(token).get(journey.readPath()).then().statusCode(200);
		sending(token, journey.writeBody()).request(journey.writeMethod(), journey.writePath())
				.then().statusCode(journey.writeStatus());
	}

	// Each grade after the first reads the grading scale back from Redis.
	@Test
	void gradeEntry_readsTheCachedGradingScaleFromRedis() {
		String token = signIn(ADMIN);
		Map<String, Object> seededExam = as(token).get("/exams?size=1").jsonPath().getMap("data.content[0]");
		String classroom = (String) seededExam.get("classroomPublicId");
		String exam = sending(token, Map.of("title", "Smoke test", "subjectPublicId", seededExam.get("subjectPublicId"),
				"classroomPublicId", classroom, "examTypePublicId", seededExam.get("examTypePublicId"),
				"scheduledAt", Instant.now().plus(1, ChronoUnit.DAYS).toString(), "maxMarks", 100))
				.post("/exams").then().statusCode(201).extract().jsonPath().getString("data.publicId");
		Response roster = as(token).get("/classrooms/" + classroom + "/students");
		List<String> students = roster.jsonPath().getList("data.content.publicId");

		assertThat(students).as(roster.asString()).hasSizeGreaterThanOrEqualTo(2);
		students.subList(0, 2).forEach(student -> sending(token,
				Map.of("studentPublicId", student, "examPublicId", exam, "marks", 75))
				.post("/grades").then().statusCode(201));
	}

	private String signIn(String email) {
		return client().contentType(ContentType.JSON)
				.body(Map.of("email", email, "password", DemoDataSeeder.PASSWORD))
				.post("/auth/login").then().statusCode(200).extract().jsonPath().getString("data.accessToken");
	}

	private RequestSpecification as(String token) {
		return client().header("Authorization", "Bearer " + token);
	}

	private RequestSpecification sending(String token, Map<String, Object> body) {
		return as(token).contentType(ContentType.JSON).header("Idempotency-Key", UUID.randomUUID().toString())
				.body(body);
	}

	private RequestSpecification client() {
		return given().port(port).basePath(BASE_PATH).header("X-Tenant-ID", tenantId);
	}

	private static Map<String, Object> ticket() {
		return Map.of("category", "TECHNICAL", "subject", "Smoke ticket", "description", "Raised by the smoke test");
	}

	private static String unique(String prefix) {
		return prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
	}
}
