package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.PeriodAttendanceService;
import com.altafjava.school.application.service.PeriodService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.application.service.TimetableService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;

/**
 * Verifies that period-attendance records created under tenant A are not visible to tenant B, and
 * that roster/timetable-entry validation behaves per {@link PeriodAttendanceService#mark}.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class PeriodAttendanceTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private PeriodAttendanceService periodAttendanceService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private PeriodService periodService;

	@Autowired
	private SubjectService subjectService;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private TimetableService timetableService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "pat-a-" + suffix, 1L, "admin@pat-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "pat-b-" + suffix, 1L, "admin@pat-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private String createAcademicYear(String name) {
		return academicYearService.create(name, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), true)
				.getPublicId().toString();
	}

	private Student enrollStudentInClassroom(String studentCode, Classroom classroom, String academicYearPublicId) {
		Student student = studentService.enroll(studentCode, "Alice", "Smith", studentCode + "@a.edu",
				LocalDate.of(2010, 1, 1));
		classroomService.enrollStudent(classroom.getPublicId().toString(), student.getPublicId().toString(),
				academicYearPublicId);
		return student;
	}

	private TimetableEntry createTimetableEntry(String suffix, Classroom classroom) {
		Period period = periodService.create("Period-" + suffix, LocalTime.of(9, 0), LocalTime.of(9, 45), 1);
		Subject subject = subjectService.create("SUB-" + suffix, "Subject " + suffix, null);
		Teacher teacher = teacherService.hire("EMP-" + suffix, "Jane", "Doe", "jane-" + suffix + "@school.test",
				LocalDate.of(2020, 1, 1));
		return timetableService.schedule(DayOfWeek.MONDAY, period.getId(), classroom.getId(), subject.getId(),
				teacher.getId(), null);
	}

	@Test
	void periodAttendanceMarkedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		Classroom classroom = classroomService.create("CLS-" + suffix, "Grade 5", "A", academicYearPublicId, null);
		Student student = enrollStudentInClassroom("STU-" + suffix, classroom, academicYearPublicId);
		TimetableEntry entry = createTimetableEntry(suffix, classroom);
		periodAttendanceService.mark(student.getId(), classroom.getId(), entry.getId(), LocalDate.now(),
				AttendanceStatus.PRESENT, "teacher-a");

		activateTenant(tenantB);
		Page<PeriodAttendance> tenantBRecords = periodAttendanceService.listAttendance(PageRequest.of(0, 100));

		boolean found = tenantBRecords.getContent().stream()
				.anyMatch(a -> tenantA.getId().equals(a.getTenantId()));
		assertFalse(found, "Tenant B must not see period attendance records created under tenant A");
	}

	@Test
	void periodAttendancePublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		Classroom classroom = classroomService.create("CLS-" + suffix, "Grade 6", "B", academicYearPublicId, null);
		Student student = enrollStudentInClassroom("STU-" + suffix, classroom, academicYearPublicId);
		TimetableEntry entry = createTimetableEntry(suffix, classroom);
		PeriodAttendance attendance = periodAttendanceService.mark(student.getId(), classroom.getId(), entry.getId(),
				LocalDate.now(), AttendanceStatus.ABSENT, "teacher-a");
		String publicId = attendance.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's period attendance");
	}

	@Test
	void mark_studentNotOnClassroomRoster_isRejected() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		Classroom classroom = classroomService.create("CLS-" + suffix, "Grade 7", "C", academicYearPublicId, null);
		Student unenrolledStudent = studentService.enroll("STU-" + suffix, "Bob", "Jones", "bob-" + suffix + "@a.edu",
				LocalDate.of(2011, 1, 1));
		TimetableEntry entry = createTimetableEntry(suffix, classroom);

		assertThrows(ResourceNotFoundException.class,
				() -> periodAttendanceService.mark(unenrolledStudent.getId(), classroom.getId(), entry.getId(),
						LocalDate.now(), AttendanceStatus.PRESENT, "teacher-a"),
				"A student not on the classroom roster must not be markable for period attendance");
	}

	@Test
	void mark_timetableEntryFromDifferentClassroom_isRejected() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		Classroom classroomA = classroomService.create("CLS-A-" + suffix, "Grade 8", "A", academicYearPublicId,
				null);
		Classroom classroomB = classroomService.create("CLS-B-" + suffix, "Grade 8", "B", academicYearPublicId,
				null);
		Student student = enrollStudentInClassroom("STU-" + suffix, classroomA, academicYearPublicId);
		TimetableEntry entryForClassroomB = createTimetableEntry(suffix, classroomB);

		assertThrows(BusinessException.class,
				() -> periodAttendanceService.mark(student.getId(), classroomA.getId(), entryForClassroomB.getId(),
						LocalDate.now(), AttendanceStatus.PRESENT, "teacher-a"),
				"A timetable entry belonging to a different classroom must be rejected");
	}
}
