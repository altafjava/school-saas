package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.util.TestPrincipals;

/**
 * Verifies that classroom rosters (student-classroom enrollment) are isolated per tenant, and
 * that {@link AttendanceService#mark} rejects a student who is not on the classroom's roster —
 * the roster-enforcement fix this table exists for (previously any student could be marked
 * present in any classroom).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class ClassroomEnrollmentTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private AttendanceService attendanceService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TestPrincipals.authenticateAsTenantAdmin();
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "scl-a-" + suffix, 1L, "admin@scl-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "scl-b-" + suffix, 1L, "admin@scl-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TestPrincipals.clear();
		TenantContext.ForTesting.clear();
	}

	private String createAcademicYear(String name) {
		AcademicYear academicYear = academicYearService.create(name, LocalDate.of(2024, 6, 1),
				LocalDate.of(2025, 5, 31), true);
		return academicYear.getPublicId().toString();
	}

	@Test
	void enrollStudent_referencingAnotherTenantsStudent_isRejected() {
		activateTenant(tenantA);
		Student studentA = studentService.enroll("STU-SCL-1", "Alice", "Smith", "alice@scl-a.test",
				LocalDate.of(2010, 1, 1));

		activateTenant(tenantB);
		Classroom classroomB = classroomService.create("CLS-SCL-1", "Grade 5", "A", createAcademicYear("2024-25"),
				null);

		assertThrows(ResourceNotFoundException.class,
				() -> classroomService.enrollStudent(classroomB.getPublicId().toString(),
						studentA.getPublicId().toString(), createAcademicYear("2024-25-b")),
				"Tenant B must not be able to enroll tenant A's student into its own classroom");
	}

	@Test
	void enrollStudent_referencingAnotherTenantsClassroom_isRejected() {
		activateTenant(tenantA);
		String academicYearPublicIdA = createAcademicYear("2024-25");
		Classroom classroomA = classroomService.create("CLS-SCL-2", "Grade 5", "A", academicYearPublicIdA, null);

		activateTenant(tenantB);
		Student studentB = studentService.enroll("STU-SCL-2", "Bob", "Jones", "bob@scl-b.test",
				LocalDate.of(2011, 3, 20));
		String academicYearPublicIdB = createAcademicYear("2024-25-b");

		assertThrows(ResourceNotFoundException.class,
				() -> classroomService.enrollStudent(classroomA.getPublicId().toString(),
						studentB.getPublicId().toString(), academicYearPublicIdB),
				"Tenant B must not be able to enroll its student into tenant A's classroom");
	}

	@Test
	void roster_isolatedPerTenant() {
		activateTenant(tenantA);
		String academicYearPublicIdA = createAcademicYear("2024-25");
		Classroom classroomA = classroomService.create("CLS-SCL-3", "Grade 5", "A", academicYearPublicIdA, null);
		Student studentA = studentService.enroll("STU-SCL-3", "Carol", "Lee", "carol@scl-a.test",
				LocalDate.of(2010, 5, 5));
		classroomService.enrollStudent(classroomA.getPublicId().toString(), studentA.getPublicId().toString(),
				academicYearPublicIdA);

		activateTenant(tenantB);
		String academicYearPublicIdB = createAcademicYear("2024-25-b");
		Classroom classroomB = classroomService.create("CLS-SCL-4", "Grade 5", "A", academicYearPublicIdB, null);
		Page<Student> rosterB = classroomService.listRoster(classroomB.getPublicId().toString(),
				PageRequest.of(0, 100));

		assertTrue(rosterB.getContent().isEmpty(), "Tenant B's classroom roster must not contain tenant A's student");
	}

	@Test
	void attendanceMark_forUnenrolledStudent_isRejected() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create("CLS-SCL-5", "Grade 5", "A", academicYearPublicId, null);
		Student student = studentService.enroll("STU-SCL-5", "Dan", "Kim", "dan@scl-a.test",
				LocalDate.of(2010, 7, 7));

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.mark(student.getPublicId().toString(), classroom.getPublicId().toString(),
						LocalDate.now(),
						AttendanceStatus.PRESENT, "teacher-a"),
				"A student not on the classroom roster must not be markable present");
	}

	@Test
	void attendanceMark_forEnrolledStudent_succeeds() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create("CLS-SCL-6", "Grade 5", "A", academicYearPublicId, null);
		Student student = studentService.enroll("STU-SCL-6", "Eve", "Wu", "eve@scl-a.test",
				LocalDate.of(2010, 9, 9));
		StudentClassroomLink link = classroomService.enrollStudent(classroom.getPublicId().toString(),
				student.getPublicId().toString(), academicYearPublicId);

		var attendance = attendanceService.mark(student.getPublicId().toString(), classroom.getPublicId().toString(),
				LocalDate.now(),
				AttendanceStatus.PRESENT, "teacher-a");

		assertEquals(student.getId(), attendance.getStudentId());
		assertEquals(classroom.getId(), link.getClassroomId());
	}
}
