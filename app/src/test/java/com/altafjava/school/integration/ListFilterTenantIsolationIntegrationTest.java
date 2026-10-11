package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.filter.CirculationFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.filter.ExamFilter;
import com.altafjava.school.application.filter.FeePaymentFilter;
import com.altafjava.school.application.filter.StudentFilter;
import com.altafjava.school.application.filter.TimetableEntryFilter;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.CirculationService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.ExamService;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.TimetableService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;

/**
 * A public id in a list filter must resolve only within the caller's tenant, including when another tenant
 * has already resolved the same id (the lookup is cached).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class ListFilterTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	private static final Pageable PAGE = PageRequest.of(0, 20);

	@Autowired
	private TenantOnboardingService onboardingService;
	@Autowired
	private StudentService studentService;
	@Autowired
	private ClassroomService classroomService;
	@Autowired
	private AcademicYearService academicYearService;
	@Autowired
	private FeePaymentService feePaymentService;
	@Autowired
	private CirculationService circulationService;
	@Autowired
	private ExamService examService;
	@Autowired
	private TimetableService timetableService;

	private Tenant tenantA;
	private Tenant tenantB;
	private String studentOfA;
	private String classroomOfA;

	@BeforeEach
	void createTenantsWithOneRecordEach() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand("School A", "flt-a-" + suffix, 1L,
				"admin@flt-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand("School B", "flt-b-" + suffix, 1L,
				"admin@flt-b.test", "Password123!", "USD"));

		activate(tenantA);
		studentOfA = studentService.enroll("STU-" + suffix, "Alice", "Smith", "alice@flt-a.test",
				LocalDate.of(2012, 5, 15)).getPublicId().toString();
		String academicYear = academicYearService
				.create("2026-27", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true).getPublicId()
				.toString();
		classroomOfA = classroomService.create("CLS-" + suffix, "Grade 5", "A", academicYear, null).getPublicId()
				.toString();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void activate(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@Test
	void studentFilter_onFeePayments_doesNotResolveAnotherTenantsStudent() {
		activate(tenantA);
		FeePaymentFilter filter = new FeePaymentFilter(studentOfA, null, DateWindow.UNBOUNDED, null);
		assertDoesNotThrow(() -> feePaymentService.listFeePayments(filter, PAGE));

		activate(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> feePaymentService.listFeePayments(filter, PAGE));
	}

	@Test
	void studentFilter_onCirculations_doesNotResolveAnotherTenantsStudent() {
		activate(tenantA);
		CirculationFilter filter = new CirculationFilter(studentOfA, null, null, DateWindow.UNBOUNDED);
		assertDoesNotThrow(() -> circulationService.list(filter, PAGE));

		activate(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> circulationService.list(filter, PAGE));
	}

	@Test
	void classroomFilter_onStudents_doesNotResolveAnotherTenantsClassroom() {
		activate(tenantA);
		StudentFilter filter = new StudentFilter(null, classroomOfA, null, null);
		assertDoesNotThrow(() -> studentService.searchStudents(filter, PAGE));

		activate(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> studentService.searchStudents(filter, PAGE));
	}

	@Test
	void classroomFilter_onExams_doesNotResolveAnotherTenantsClassroom() {
		activate(tenantA);
		ExamFilter filter = new ExamFilter(classroomOfA, null, null, null);
		assertDoesNotThrow(() -> examService.listExams(filter, PAGE));

		activate(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> examService.listExams(filter, PAGE));
	}

	@Test
	void classroomFilter_onTimetable_doesNotResolveAnotherTenantsClassroom() {
		activate(tenantA);
		TimetableEntryFilter filter = new TimetableEntryFilter(classroomOfA, null, null, null);
		assertDoesNotThrow(() -> timetableService.listEntries(filter, PAGE));

		activate(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> timetableService.listEntries(filter, PAGE));
	}
}
