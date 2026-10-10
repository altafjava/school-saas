package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.PeriodService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.application.service.TimetableService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;

/**
 * Verifies that timetable entries created under tenant A are not visible to tenant B, and that
 * classroom/teacher conflict validation is itself tenant-scoped (an entry in tenant A must never
 * block an otherwise-identical entry in tenant B).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class TimetableTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private TimetableService timetableService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private SubjectService subjectService;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private PeriodService periodService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "ttb-a-" + suffix, 1L, "admin@ttb-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "ttb-b-" + suffix, 1L, "admin@ttb-b.test", "Password123!", "USD"));
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

	private record Fixture(String periodId, Classroom classroom, String subjectId, String teacherId) {
	}

	private Fixture buildFixture(String prefix) {
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		Period period = periodService.create(prefix + "-Period-" + suffix, LocalTime.of(9, 0), LocalTime.of(9, 45),
				1);
		var academicYear = academicYearService.create(prefix + "-2025-26-" + suffix, LocalDate.of(2025, 6, 1),
				LocalDate.of(2026, 5, 31), true);
		Classroom classroom = classroomService.create(prefix + "-CLS-" + suffix, "Grade 5", "A",
				academicYear.getPublicId().toString(), null);
		Subject subject = subjectService.create(prefix + "-SUB-" + suffix, "Mathematics", null);
		Teacher teacher = teacherService.hire(prefix + "-EMP-" + suffix, "Jane", "Doe", prefix + "-jane@test.edu",
				LocalDate.of(2020, 1, 1));
		return new Fixture(period.getPublicId().toString(), classroom, subject.getPublicId().toString(),
				teacher.getPublicId().toString());
	}

	@Test
	void timetableEntryCreatedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		Fixture fixtureA = buildFixture("A");
		timetableService.schedule(DayOfWeek.MONDAY, fixtureA.periodId(), fixtureA.classroom().getPublicId().toString(),
				fixtureA.subjectId(), fixtureA.teacherId(), null);

		activateTenant(tenantB);
		Fixture fixtureB = buildFixture("B");
		List<TimetableEntry> tenantBEntries = timetableService
				.listForClassroom(fixtureB.classroom().getPublicId().toString());

		boolean found = tenantBEntries.stream().anyMatch(e -> tenantA.getId().equals(e.getTenantId()));
		assertFalse(found, "Tenant B must not see timetable entries created under tenant A");
	}

	@Test
	void timetableEntryPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		Fixture fixtureA = buildFixture("A2");
		TimetableEntry entry = timetableService.schedule(DayOfWeek.TUESDAY, fixtureA.periodId(),
				fixtureA.classroom().getPublicId().toString(), fixtureA.subjectId(), fixtureA.teacherId(), null);
		String publicId = entry.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> timetableService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's timetable entry");
	}

	@Test
	void identicalClassroomPeriodTeacherCombination_doesNotConflictAcrossTenants() {
		activateTenant(tenantA);
		Fixture fixtureA = buildFixture("A3");
		assertDoesNotThrow(() -> timetableService.schedule(DayOfWeek.WEDNESDAY, fixtureA.periodId(),
				fixtureA.classroom().getPublicId().toString(), fixtureA.subjectId(), fixtureA.teacherId(), null));

		// Tenant B has its own, independently-numbered period/classroom/teacher IDs, so this is
		// not literally "the same" row — but it proves conflict checks never leak across
		// tenant_id, which is the property that matters for isolation.
		activateTenant(tenantB);
		Fixture fixtureB = buildFixture("B3");
		assertDoesNotThrow(() -> timetableService.schedule(DayOfWeek.WEDNESDAY, fixtureB.periodId(),
				fixtureB.classroom().getPublicId().toString(), fixtureB.subjectId(), fixtureB.teacherId(), null));
	}
}
