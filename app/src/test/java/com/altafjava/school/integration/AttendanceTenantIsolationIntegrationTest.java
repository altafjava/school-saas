package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
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
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that attendance records created under tenant A are not visible to tenant B.
 *
 * Phase 5 validation: multi-tenant data isolation for the Attendance entity.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class AttendanceTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private AttendanceService attendanceService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "att-a-" + suffix, 1L, "admin@att-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "att-b-" + suffix, 1L, "admin@att-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
		authenticateAsTenantAdmin();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	// calculatePercentage() routes through StudentDataAccessGuard, which requires a real authenticated
	// principal — TENANT_ADMIN bypasses the guard's ownership check, matching how staff actually call it.
	private void authenticateAsTenantAdmin() {
		AuthenticatedUser principal = new AuthenticatedUser() {
			@Override
			public Long getId() {
				return -1L;
			}

			@Override
			public String getUsername() {
				return "admin";
			}

			@Override
			public Long getTenantId() {
				return null;
			}
		};
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private String createAcademicYear(String name) {
		AcademicYear academicYear = academicYearService.create(name, LocalDate.of(2024, 6, 1),
				LocalDate.of(2025, 5, 31), true);
		return academicYear.getPublicId().toString();
	}

	private Student enrollStudentInClassroom(String studentCode, String firstName, String lastName, String email,
			Classroom classroom, String academicYearPublicId) {
		Student student = studentService.enroll(studentCode, firstName, lastName, email, LocalDate.of(2010, 1, 1));
		classroomService.enrollStudent(classroom.getPublicId().toString(), student.getPublicId().toString(),
				academicYearPublicId);
		return student;
	}

	@Test
	void attendanceMarkedUnderTenantA_isNotVisibleToTenantB() {
		// Given — create classroom and student under tenant A, mark attendance
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		String classCode = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		var classroom = classroomService.create(classCode, "Grade 5", "A", academicYearPublicId, null);
		String studentCode = "STU-" + UUID.randomUUID().toString().substring(0, 6);
		Student student = enrollStudentInClassroom(studentCode, "Alice", "Smith", "alice@a.edu", classroom,
				academicYearPublicId);
		attendanceService.mark(student.getPublicId().toString(), classroom.getPublicId().toString(), LocalDate.now(),
				AttendanceStatus.PRESENT, "teacher-a");

		// When — tenant B lists attendance
		activateTenant(tenantB);
		Page<Attendance> tenantBAttendance = attendanceService.listAttendance(PageRequest.of(0, 100));

		// Then — tenant B must not see tenant A's records
		boolean found = tenantBAttendance.getContent().stream()
				.anyMatch(a -> tenantA.getId().equals(a.getTenantId()));
		assertFalse(found, "Tenant B must not see attendance records created under tenant A");
	}

	@Test
	void attendancePublicId_notAccessibleAcrossTenants() {
		// Given — mark attendance under tenant A
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		String classCode = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		var classroom = classroomService.create(classCode, "Grade 6", "B", academicYearPublicId, null);
		Student student = enrollStudentInClassroom("STU-" + UUID.randomUUID().toString().substring(0, 6),
				"Bob", "Jones", "bob@a.edu", classroom, academicYearPublicId);
		Attendance attendance = attendanceService.mark(student.getPublicId().toString(),
				classroom.getPublicId().toString(),
				LocalDate.now().minusDays(1), AttendanceStatus.ABSENT, "teacher-a");
		String publicId = attendance.getPublicId().toString();

		// When/Then — tenant B cannot fetch tenant A's attendance record
		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's attendance");
	}

	@Test
	void attendancePercentage_countsOnlyMarkedDaysWithinRange() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		String classCode = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		var classroom = classroomService.create(classCode, "Grade 5", "C", academicYearPublicId, null);
		Student student = enrollStudentInClassroom("STU-" + UUID.randomUUID().toString().substring(0, 6),
				"Carol", "Diaz", "carol@a.edu", classroom, academicYearPublicId);
		LocalDate day1 = LocalDate.now().minusDays(2);
		LocalDate day2 = LocalDate.now().minusDays(1);
		LocalDate day3 = LocalDate.now();
		attendanceService.mark(student.getPublicId().toString(), classroom.getPublicId().toString(), day1,
				AttendanceStatus.PRESENT, "teacher-a");
		attendanceService.mark(student.getPublicId().toString(), classroom.getPublicId().toString(), day2,
				AttendanceStatus.ABSENT, "teacher-a");
		attendanceService.mark(student.getPublicId().toString(), classroom.getPublicId().toString(), day3,
				AttendanceStatus.PRESENT, "teacher-a");

		AttendancePercentage result = attendanceService.calculatePercentage(student.getPublicId().toString(), day1,
				day3);

		assertEquals(2, result.presentDays());
		assertEquals(3, result.totalMarkedDays());
	}

	@Test
	void attendancePercentage_forTenantAStudent_notAccessibleFromTenantB() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		String classCode = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		var classroom = classroomService.create(classCode, "Grade 6", "D", academicYearPublicId, null);
		Student student = enrollStudentInClassroom("STU-" + UUID.randomUUID().toString().substring(0, 6),
				"Dan", "Lee", "dan@a.edu", classroom, academicYearPublicId);
		String studentPublicId = student.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.calculatePercentage(studentPublicId, LocalDate.now().minusDays(5),
						LocalDate.now()),
				"Tenant B must receive ResourceNotFoundException for tenant A's student");
	}
}
