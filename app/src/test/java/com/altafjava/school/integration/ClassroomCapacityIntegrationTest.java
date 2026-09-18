package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies real capacity enforcement on classroom enrollment — mirrors
 * {@code RoomAllocationService}'s existing capacity guard on {@code Room}, and that a null
 * capacity (every classroom created before this field existed) keeps meaning "unlimited".
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class ClassroomCapacityIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private StudentService studentService;

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
				"Capacity School A", "cap-a-" + suffix, 1L, "admin@cap-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Capacity School B", "cap-b-" + suffix, 1L, "admin@cap-b.test", "Password123!", "USD"));
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
		AcademicYear academicYear = academicYearService.create(name, LocalDate.of(2024, 6, 1),
				LocalDate.of(2025, 5, 31), true);
		return academicYear.getPublicId().toString();
	}

	@Test
	void enrollStudent_atCapacity_isRejected() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create("CLS-CAP-1", "Grade 5", "A", academicYearPublicId, null);
		classroomService.updateCapacity(classroom.getPublicId().toString(), 1);

		Student first = studentService.enroll("STU-CAP-1", "Alice", "Smith", "alice@cap-a.test",
				LocalDate.of(2010, 1, 1));
		classroomService.enrollStudent(classroom.getPublicId().toString(), first.getPublicId().toString(),
				academicYearPublicId);

		Student second = studentService.enroll("STU-CAP-2", "Bob", "Jones", "bob@cap-a.test",
				LocalDate.of(2010, 2, 2));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> classroomService.enrollStudent(classroom.getPublicId().toString(),
						second.getPublicId().toString(), academicYearPublicId),
				"A classroom at capacity must reject further enrollment");
		assertEquals("Classroom " + classroom.getPublicId() + " is at full capacity", exception.getMessage());
	}

	@Test
	void enrollStudent_withNoCapacitySet_isUnlimited() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create("CLS-CAP-3", "Grade 5", "A", academicYearPublicId, null);

		Student first = studentService.enroll("STU-CAP-3", "Carol", "Lee", "carol@cap-a.test",
				LocalDate.of(2010, 3, 3));
		Student second = studentService.enroll("STU-CAP-4", "Dan", "Kim", "dan@cap-a.test",
				LocalDate.of(2010, 4, 4));

		classroomService.enrollStudent(classroom.getPublicId().toString(), first.getPublicId().toString(),
				academicYearPublicId);
		classroomService.enrollStudent(classroom.getPublicId().toString(), second.getPublicId().toString(),
				academicYearPublicId);
	}

	@Test
	void capacity_isIsolatedPerTenant() {
		activateTenant(tenantA);
		String academicYearPublicIdA = createAcademicYear("2024-25");
		Classroom classroomA = classroomService.create("CLS-CAP-5", "Grade 5", "A", academicYearPublicIdA, null);
		classroomService.updateCapacity(classroomA.getPublicId().toString(), 1);
		Student studentA1 = studentService.enroll("STU-CAP-5", "Eve", "Wu", "eve@cap-a.test",
				LocalDate.of(2010, 5, 5));
		classroomService.enrollStudent(classroomA.getPublicId().toString(), studentA1.getPublicId().toString(),
				academicYearPublicIdA);

		activateTenant(tenantB);
		String academicYearPublicIdB = createAcademicYear("2024-25-b");
		Classroom classroomB = classroomService.create("CLS-CAP-6", "Grade 5", "A", academicYearPublicIdB, null);
		Student studentB1 = studentService.enroll("STU-CAP-6", "Frank", "Ng", "frank@cap-b.test",
				LocalDate.of(2010, 6, 6));

		// Tenant A's classroom being at capacity must not affect tenant B's — a fresh classroom in
		// a different tenant is unaffected even though it shares no isolation mechanism beyond
		// tenant_id scoping on the roster count query.
		classroomService.enrollStudent(classroomB.getPublicId().toString(), studentB1.getPublicId().toString(),
				academicYearPublicIdB);
	}

	@Test
	void enrollStudent_afterWithdrawal_succeedsInADifferentClassroom_sameAcademicYear() {
		// Regression: uq_scl_student_academic_year enforced uniqueness over every row regardless
		// of the soft-delete flag, so withdrawing a student and re-enrolling them (even into a
		// different classroom) for the same academic year hit a DB constraint violation against
		// their own withdrawn row. See 051-classroom-link-soft-delete-unique-fix.xml.
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroomX = classroomService.create("CLS-CAP-7", "Grade 5", "A", academicYearPublicId, null);
		Classroom classroomY = classroomService.create("CLS-CAP-8", "Grade 5", "B", academicYearPublicId, null);
		Student student = studentService.enroll("STU-CAP-7", "Grace", "Ho", "grace@cap-a.test",
				LocalDate.of(2010, 7, 7));

		classroomService.enrollStudent(classroomX.getPublicId().toString(), student.getPublicId().toString(),
				academicYearPublicId);
		classroomService.withdrawStudentFromClassroom(classroomX.getPublicId().toString(),
				student.getPublicId().toString());

		classroomService.enrollStudent(classroomY.getPublicId().toString(), student.getPublicId().toString(),
				academicYearPublicId);
	}

	@Test
	void enrollStudent_whileAlreadyActivelyEnrolled_isRejected() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroomX = classroomService.create("CLS-CAP-9", "Grade 5", "A", academicYearPublicId, null);
		Classroom classroomY = classroomService.create("CLS-CAP-10", "Grade 5", "B", academicYearPublicId, null);
		Student student = studentService.enroll("STU-CAP-8", "Henry", "Ito", "henry@cap-a.test",
				LocalDate.of(2010, 8, 8));

		classroomService.enrollStudent(classroomX.getPublicId().toString(), student.getPublicId().toString(),
				academicYearPublicId);

		assertThrows(BusinessException.class,
				() -> classroomService.enrollStudent(classroomY.getPublicId().toString(),
						student.getPublicId().toString(), academicYearPublicId),
				"A student already actively enrolled for this academic year must not be enrolled again");
	}
}
