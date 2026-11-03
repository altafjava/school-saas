package com.altafjava.school.realinfrastructure;

import static io.restassured.RestAssured.given;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.base.SchoolInfrastructureTestBase;
import com.altafjava.school.demo.DemoDataSeeder;
import io.restassured.http.ContentType;

/**
 * Every role against the live application: what each may read, and what each must be refused. A
 * denial is the point here, so an endpoint a role could reach by accident fails the build.
 */
class RoleAccessMatrixTest extends SchoolInfrastructureTestBase {

	private static final String ADMIN = "admin@demo.school";
	private static final String PRINCIPAL = "principal@demo.school";
	private static final String FINANCE = "finance@demo.school";
	private static final String HR = "hr@demo.school";
	private static final String ACADEMIC = "academic@demo.school";
	private static final String TEACHER = "teacher@demo.school";
	private static final String STUDENT = "student@demo.school";
	private static final String PARENT = "parent@demo.school";
	private static final List<String> EVERYONE = List.of(ADMIN, PRINCIPAL, FINANCE, HR, ACADEMIC, TEACHER, STUDENT,
			PARENT);

	private record Rule(String path, Set<String> allowed) {
	}

	private static final List<Rule> MATRIX = List.of(
			new Rule("/me", Set.copyOf(EVERYONE)),
			new Rule("/me/tenants", Set.copyOf(EVERYONE)),
			new Rule("/dashboards/principal", Set.of(ADMIN, PRINCIPAL)),
			new Rule("/dashboards/finance", Set.of(ADMIN, FINANCE)),
			new Rule("/dashboards/hr", Set.of(ADMIN, HR)),
			new Rule("/dashboards/academic", Set.of(ADMIN, ACADEMIC)),
			new Rule("/me/home/teacher", Set.of(TEACHER)),
			new Rule("/me/home/parent", Set.of(PARENT)),
			new Rule("/me/home/student", Set.of(STUDENT)),
			new Rule("/staff-attendance/register?date=2026-01-05", Set.of(ADMIN, PRINCIPAL, HR)),
			new Rule("/staff-attendance/me", Set.of(TEACHER)),
			new Rule("/fee-invoices", Set.of(ADMIN, FINANCE)),
			new Rule("/enquiries", Set.of(ADMIN, PRINCIPAL)),
			new Rule("/imports", Set.of(ADMIN)),
			new Rule("/billing/data-export/status", Set.of(ADMIN)),
			new Rule("/billing/payment-methods", Set.of(ADMIN)),
			new Rule("/billing/invoices", Set.of(ADMIN)),
			new Rule("/billing/receipts", Set.of(ADMIN)),
			new Rule("/billing/contacts", Set.of(ADMIN)),
			new Rule("/billing/address", Set.of(ADMIN)),
			new Rule("/billing/tax-id", Set.of(ADMIN)),
			new Rule("/tenant-settings/integration-providers", Set.of(ADMIN)),
			new Rule("/admin/tenant-deletions", Set.of()),
			new Rule("/admin/integration-providers", Set.of()),
			new Rule("/admin/product", Set.of()));

	@LocalServerPort
	private int port;
	@Autowired
	private TenantRepository tenantRepository;
	@Autowired
	private StringRedisTemplate redis;

	private Long tenantId;

	@BeforeEach
	void resolveTenant() {
		tenantId = tenantRepository.findBySubdomain(DemoDataSeeder.SUBDOMAIN).orElseThrow().getId();
		redis.delete(redis.keys("endpoint:LOGIN:*"));
	}

	static Stream<Arguments> matrix() {
		return MATRIX.stream().flatMap(rule -> EVERYONE.stream()
				.map(email -> Arguments.of(email, rule.path(), rule.allowed().contains(email))));
	}

	@ParameterizedTest(name = "{0} {1} allowed={2}")
	@MethodSource("matrix")
	void role_getsExactlyTheAccessItShould(String email, String path, boolean allowed) {
		given().baseUri("http://localhost").port(port).header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + signIn(email))
				.when().get("/api/v1" + path)
				.then().statusCode(allowed ? 200 : 403);
	}

	private String signIn(String email) {
		return given().baseUri("http://localhost").port(port).header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body(Map.of("email", email, "password", DemoDataSeeder.PASSWORD))
				.when().post("/api/v1/auth/login")
				.then().statusCode(200)
				.extract().path("data.accessToken");
	}
}
