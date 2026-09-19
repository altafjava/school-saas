package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.organization.CreateOrganizationCommand;
import com.altafjava.platform.application.organization.OrganizationService;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.organization.model.Organization;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.rollup.OrganizationRollupService;
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
import com.altafjava.school.domain.rollup.model.OrganizationRollupReport;
import com.altafjava.school.domain.student.model.Student;

/**
 * Validates ROADMAP.md Phase 4's "multi-campus rollup is actually usable" outcome end to end at
 * the service layer: two real campuses under one real {@code Organization}, each with its own
 * students/attendance/fees, aggregated by {@link OrganizationRollupService} with no cross-tenant
 * leakage or double counting.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class OrganizationRollupIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private OrganizationService organizationService;

	@Autowired
	private OrganizationRollupService organizationRollupService;

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

	@BeforeEach
	void createOrganizationWithTwoCampuses() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		campusA = onboardingService.registerTenant(new RegisterTenantCommand(
				"Rollup Campus A", "rollup-a-" + suffix, 1L, "admin@rollup-a.test", "Password123!", "USD"));
		campusB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Rollup Campus B", "rollup-b-" + suffix, 1L, "admin@rollup-b.test", "Password123!", "USD"));

		organization = organizationService.createOrganization(
				new CreateOrganizationCommand("Rollup Test Group", "rollup-group-" + suffix,
						"contact@rollup-group.test", null, null));
		organizationService.addTenantToOrganization(organization.getPublicId(), campusA.getPublicId());
		organizationService.addTenantToOrganization(organization.getPublicId(), campusB.getPublicId());
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void activate(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private void seedCampus(Tenant campus, int studentCount, BigDecimal feeAmount, BigDecimal paidPerStudent) {
		activate(campus);
		var academicYear = academicYearService.create("2024-25-" + UUID.randomUUID().toString().substring(0, 6),
				LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), true);
		String classCode = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		var classroom = classroomService.create(classCode, "Grade 5", "A", academicYear.getPublicId().toString(),
				null);
		FeeStructure feeStructure = feeStructureService.create(
				"Tuition-" + UUID.randomUUID().toString().substring(0, 6), feeAmount, FeeFrequency.MONTHLY,
				"Standard");
		// A FeeStructure only counts toward a student's total once a FeeAssignment actually
		// applies it — classroom-scoped here since every student enrolled below joins this one
		// classroom.
		feeAssignmentService.assign(feeStructure.getPublicId().toString(), null, classroom.getPublicId().toString());

		for (int i = 0; i < studentCount; i++) {
			String studentCode = "STU-" + UUID.randomUUID().toString().substring(0, 8);
			Student student = studentService.enroll(studentCode, "First" + i, "Last" + i,
					"student" + UUID.randomUUID().toString().substring(0, 6) + "@campus.test",
					LocalDate.of(2012, 1, 1));
			classroomService.enrollStudent(classroom.getPublicId().toString(), student.getPublicId().toString(),
					academicYear.getPublicId().toString());
			attendanceService.mark(student.getId(), classroom.getId(), LocalDate.of(2026, 1, 10),
					AttendanceStatus.PRESENT, "teacher");
			if (paidPerStudent.signum() > 0) {
				feePaymentService.record(student.getId(), feeStructure.getId(), paidPerStudent,
						LocalDateTime.of(2026, 1, 15, 9, 0), "RCPT-" + UUID.randomUUID().toString().substring(0, 10));
			}
		}
		TenantContext.ForTesting.clear();
	}

	@Test
	void generate_aggregatesRealDataAcrossBothCampusesWithoutLeakageOrDoubleCounting() {
		seedCampus(campusA, 3, BigDecimal.valueOf(500), BigDecimal.valueOf(200));
		seedCampus(campusB, 2, BigDecimal.valueOf(300), BigDecimal.valueOf(300));

		OrganizationRollupReport report = organizationRollupService.generate(
				organization.getPublicId().toString(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

		assertEquals(2, report.campuses().size());
		assertEquals(5, report.totals().activeStudentCount());
		// Both campuses marked all their students PRESENT once inside the report window.
		assertEquals(5, report.totals().attendance().present());
		assertEquals(0, report.totals().attendance().absent());

		// campusA: 3 students * 500 due = 1500 due, 3 * 200 paid = 600 paid -> 900 outstanding
		// campusB: 2 students * 300 due = 600 due, 2 * 300 paid = 600 paid -> fully paid
		assertMoneyEquals(2100, report.totals().fees().totalDue());
		assertMoneyEquals(1200, report.totals().fees().totalPaid());
		assertMoneyEquals(900, report.totals().fees().outstandingBalance());

		var campusAReport = report.campuses().stream()
				.filter(c -> c.tenantPublicId().equals(campusA.getPublicId())).findFirst().orElseThrow();
		var campusBReport = report.campuses().stream()
				.filter(c -> c.tenantPublicId().equals(campusB.getPublicId())).findFirst().orElseThrow();

		assertEquals(3, campusAReport.activeStudentCount());
		assertEquals(2, campusBReport.activeStudentCount());
		assertMoneyEquals(900, campusAReport.fees().outstandingBalance());
		assertMoneyEquals(0, campusBReport.fees().outstandingBalance());
	}

	// FeePayment.paidAmount is a DECIMAL column — SUM() returns a scaled value (e.g. "2100.00"),
	// so BigDecimal.equals (which compares scale, not just numeric value) would spuriously fail
	// against a scale-0 literal like BigDecimal.valueOf(2100).
	private void assertMoneyEquals(long expected, BigDecimal actual) {
		assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
				() -> "expected " + expected + " but was " + actual);
	}

	@Test
	void generate_organizationWithNoCampusesAdded_returnsEmptyReport() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		Organization emptyOrg = organizationService.createOrganization(
				new CreateOrganizationCommand("Empty Group", "empty-group-" + suffix, "contact@empty.test", null,
						null));

		OrganizationRollupReport report = organizationRollupService.generate(
				emptyOrg.getPublicId().toString(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

		assertEquals(0, report.campuses().size());
		assertEquals(0, report.totals().activeStudentCount());
	}
}
