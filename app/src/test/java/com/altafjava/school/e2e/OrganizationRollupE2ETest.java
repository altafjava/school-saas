package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.organization.CreateOrganizationCommand;
import com.altafjava.platform.application.organization.OrganizationService;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.organization.model.Organization;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.FeeAssignmentService;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation (here: organization-not-found rather than cross-tenant 404, since this endpoint
 * is inherently cross-tenant by design).
 *
 * <p>
 * The two {@code rollupHappyPath_*} tests are also the regression tests for the real hazard
 * documented on {@link com.altafjava.school.application.rollup.OrganizationRollupService}: a
 * SUPER_ADMIN (system-tenant) token resolves with no Hibernate {@code tenantFilter} bound for the
 * request, but a real tenant user like {@code ORG_ADMIN} does get the filter bound to their home
 * tenant — without {@code TenantFilterSwitcher} explicitly re-pointing it per campus, the
 * per-campus {@code TenantContext.callAsTenant} loop would silently return zero rows for every
 * campus but the caller's own. Only an E2E test that goes through the real filter chain can catch
 * that regression — the service-layer integration test cannot, since it never engages
 * {@code TenantContextFilter}/{@code TenantFilterRequestFilter}.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganizationRollupE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private OrganizationService organizationService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private StudentService studentService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private AttendanceService attendanceService;

	@Autowired
	private FeeStructureService feeStructureService;

	@Autowired
	private FeeAssignmentService feeAssignmentService;

	@Autowired
	private FeePaymentService feePaymentService;

	private Tenant campusA;
	private Tenant campusB;
	private Organization organization;
	private String superAdminToken;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		TenantContext.ForTesting.clear();

		String suffix = UUID.randomUUID().toString().substring(0, 8);
		campusA = onboardingService.registerTenant(new RegisterTenantCommand(
				"E2E Rollup Campus A", "e2e-rollup-a-" + suffix, 1L, "admin@e2e-rollup-a.test", "Password123!",
				"USD"));
		campusB = onboardingService.registerTenant(new RegisterTenantCommand(
				"E2E Rollup Campus B", "e2e-rollup-b-" + suffix, 1L, "admin@e2e-rollup-b.test", "Password123!",
				"USD"));
		organization = organizationService.createOrganization(new CreateOrganizationCommand(
				"E2E Rollup Group", "e2e-rollup-group-" + suffix, "contact@e2e-rollup-group.test", null, null));
		organizationService.addTenantToOrganization(organization.getPublicId(), campusA.getPublicId());
		organizationService.addTenantToOrganization(organization.getPublicId(), campusB.getPublicId());

		seedCampus(campusA, 2, BigDecimal.valueOf(400), BigDecimal.valueOf(100));
		seedCampus(campusB, 1, BigDecimal.valueOf(600), BigDecimal.valueOf(600));

		TenantContext.ForTesting.clear();
		superAdminToken = authHelper.tokenWithRole(TenantContext.SYSTEM_TENANT_ID, "SUPER_ADMIN");
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void seedCampus(Tenant campus, int studentCount, BigDecimal feeAmount, BigDecimal paidPerStudent) {
		TenantContext.ForTesting.setCurrentTenant(campus.getId(), campus.getPublicId(), campus.getSubdomain(),
				campus.getType());
		var academicYear = academicYearService.create("2024-25-" + UUID.randomUUID().toString().substring(0, 6),
				LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), true);
		String classCode = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		var classroom = classroomService.create(classCode, "Grade 4", "A", academicYear.getPublicId().toString(),
				null);
		FeeStructure feeStructure = feeStructureService.create(
				"Tuition-" + UUID.randomUUID().toString().substring(0, 6), feeAmount, FeeFrequency.MONTHLY,
				"Standard");
		// A FeeStructure only counts toward a student's total once a FeeAssignment actually
		// applies it — classroom-scoped here since every student enrolled below joins this one
		// classroom.
		feeAssignmentService.assign(feeStructure.getPublicId().toString(), null, classroom.getPublicId().toString());
		for (int i = 0; i < studentCount; i++) {
			Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 8),
					"First" + i, "Last" + i,
					"student" + UUID.randomUUID().toString().substring(0, 6) + "@campus.test",
					LocalDate.of(2012, 1, 1));
			classroomService.enrollStudent(classroom.getPublicId().toString(), student.getPublicId().toString(),
					academicYear.getPublicId().toString());
			attendanceService.mark(student.getId(), classroom.getId(), LocalDate.of(2026, 2, 10),
					AttendanceStatus.PRESENT, "teacher");
			feePaymentService.record(student.getId(), feeStructure.getId(), paidPerStudent,
					LocalDateTime.of(2026, 2, 12, 9, 0), "RCPT-" + UUID.randomUUID().toString().substring(0, 10));
		}
		TenantContext.ForTesting.clear();
	}

	private String rollupPath() {
		return "/api/v1/organizations/" + organization.getPublicId() + "/rollup-report?from=2026-02-01&to=2026-02-28";
	}

	@Test
	void rollupHappyPath_asSuperAdmin_aggregatesBothCampusesCorrectly() {
		given()
				.header("Authorization", "Bearer " + superAdminToken)
				.contentType(ContentType.JSON)
				.when()
				.get(rollupPath())
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.organizationPublicId", equalTo(organization.getPublicId().toString()))
				.body("data.campuses", hasSize(2))
				// campusA: 2 students, 2*400=800 due, 2*100=200 paid -> 600 outstanding
				// campusB: 1 student, 1*600=600 due, 1*600=600 paid -> fully paid
				.body("data.totals.activeStudentCount", equalTo(3))
				.body("data.totals.attendance.present", equalTo(3))
				.body("data.totals.fees.totalDue", comparesEqualTo(1400.00f))
				.body("data.totals.fees.totalPaid", comparesEqualTo(800.00f))
				.body("data.totals.fees.outstandingBalance", comparesEqualTo(600.00f));
	}

	@Test
	void rollupHappyPath_asOrgAdminForTheirOwnOrganization_aggregatesBothCampusesCorrectly() {
		// Regression test for the Hibernate tenantFilter fix (OrganizationRollupService /
		// TenantFilterSwitcher): unlike the SUPER_ADMIN token above, an ORG_ADMIN is a real tenant
		// user — their request DOES get the filter bound to their home tenant (campusA). Without
		// the fix, campusB's rows would be silently ANDed out and every total below would be wrong.
		String orgAdminToken = authHelper.tokenForOrgMember(campusA.getId(), 1L, "org-admin@e2e-rollup.test",
				"ORG_ADMIN", organization.getId());

		given()
				.header("X-Tenant-ID", campusA.getId())
				.header("Authorization", "Bearer " + orgAdminToken)
				.contentType(ContentType.JSON)
				.when()
				.get(rollupPath())
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.campuses", hasSize(2))
				.body("data.totals.activeStudentCount", equalTo(3))
				.body("data.totals.fees.totalDue", comparesEqualTo(1400.00f))
				.body("data.totals.fees.totalPaid", comparesEqualTo(800.00f));
	}

	@Test
	void rollup_asOrgAdminForADifferentOrganization_returns403() {
		String orgAdminToken = authHelper.tokenForOrgMember(campusA.getId(), 1L, "org-admin@e2e-rollup.test",
				"ORG_ADMIN", organization.getId() + 999_999L);

		given()
				.header("X-Tenant-ID", campusA.getId())
				.header("Authorization", "Bearer " + orgAdminToken)
				.contentType(ContentType.JSON)
				.when()
				.get(rollupPath())
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void rollup_withoutJwt_returns401() {
		given()
				.contentType(ContentType.JSON)
				.when()
				.get(rollupPath())
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void rollup_asTenantAdminRole_returns403() {
		String tenantAdminToken = authHelper.tokenWithRole(campusA.getId(), "TENANT_ADMIN");

		given()
				.header("X-Tenant-ID", campusA.getId())
				.header("Authorization", "Bearer " + tenantAdminToken)
				.contentType(ContentType.JSON)
				.when()
				.get(rollupPath())
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void rollup_unknownOrganization_returns404() {
		String unknownOrgPath = "/api/v1/organizations/" + UUID.randomUUID() + "/rollup-report"
				+ "?from=2026-02-01&to=2026-02-28";

		given()
				.header("Authorization", "Bearer " + superAdminToken)
				.contentType(ContentType.JSON)
				.when()
				.get(unknownOrgPath)
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void rollup_toBeforeFrom_returns400() {
		String invalidRangePath = "/api/v1/organizations/" + organization.getPublicId() + "/rollup-report"
				+ "?from=2026-02-28&to=2026-02-01";

		given()
				.header("Authorization", "Bearer " + superAdminToken)
				.contentType(ContentType.JSON)
				.when()
				.get(invalidRangePath)
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}
}
