package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
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
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "att-a-" + suffix, 1L, "admin@att-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "att-b-" + suffix, 1L, "admin@att-b.test", "Password123!", "USD"));
		TenantContext.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(), tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.clear();
	}

	@Test
	void attendanceMarkedUnderTenantA_isNotVisibleToTenantB() {
		// Given — create classroom and student under tenant A, mark attendance
		activateTenant(tenantA);
		String classCode = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		var classroom = classroomService.create(classCode, "Grade 5", "A", "2024-25", null);
		String studentCode = "STU-" + UUID.randomUUID().toString().substring(0, 6);
		Student student = studentService.enroll(studentCode, "Alice", "Smith", "alice@a.edu", LocalDate.of(2010, 1, 1));
		attendanceService.mark(student.getId(), classroom.getId(), LocalDate.now(),
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
		String classCode = "CLS-" + UUID.randomUUID().toString().substring(0, 6);
		var classroom = classroomService.create(classCode, "Grade 6", "B", "2024-25", null);
		Student student = studentService.enroll(
				"STU-" + UUID.randomUUID().toString().substring(0, 6),
				"Bob", "Jones", "bob@a.edu", LocalDate.of(2011, 3, 20));
		Attendance attendance = attendanceService.mark(student.getId(), classroom.getId(),
				LocalDate.now().minusDays(1), AttendanceStatus.ABSENT, "teacher-a");
		String publicId = attendance.getPublicId().toString();

		// When/Then — tenant B cannot fetch tenant A's attendance record
		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's attendance");
	}
}
