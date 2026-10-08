package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.EmployeeService;
import com.altafjava.school.application.service.PayslipService;
import com.altafjava.school.application.service.SalaryStructureService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.payroll.model.Payslip;
import com.altafjava.school.domain.payroll.repository.PayComponentDefinitionRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * Employee as the HR master record with Teacher as its subtype, against the real database: JOINED
 * inheritance behaves (same id and public id, soft-delete applies to both), non-teaching staff get
 * payroll, only teachers can run classes, and leavers drop out of the right places.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class EmployeeIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private EmployeeService employeeService;
	@Autowired
	private TeacherService teacherService;
	@Autowired
	private EmployeeRepository employeeRepository;
	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private SalaryStructureService salaryStructureService;
	@Autowired
	private PayslipService payslipService;
	@Autowired
	private ClassroomService classroomService;
	@Autowired
	private AcademicYearService academicYearService;
	@Autowired
	private PayComponentDefinitionRepository payComponentDefinitionRepository;
	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenant;

	@BeforeEach
	void setUp() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand("Staff School", "emp-" + suffix, 1L,
				"admin@emp-" + suffix + ".test", "Password123!", "USD"));
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
		awaitSeededPayComponents();
	}

	// Tenant provisioning seeds the pay components asynchronously; payroll needs them to exist.
	private void awaitSeededPayComponents() {
		long deadline = System.currentTimeMillis() + 20_000;
		while (!payComponentDefinitionRepository.existsByCodeAndTenantId("BASIC", tenant.getId())) {
			if (System.currentTimeMillis() > deadline) {
				throw new AssertionError("Pay components were not seeded within 20s");
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError(e);
			}
		}
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private String code() {
		return "EMP-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private Employee hireDriver() {
		return employeeService.hire(StaffCategory.SUPPORT, code(), "Sam", "Driver", "sam-" + code() + "@school.test",
				LocalDate.of(2024, 1, 1));
	}

	private Teacher hireTeacher() {
		return teacherService.hire(code(), "Jane", "Doe", "jane-" + code() + "@school.test", LocalDate.of(2020, 1, 1));
	}

	@Test
	void aTeacherIsAnEmployeeWithTheSameIdAndPublicId() {
		Teacher teacher = hireTeacher();

		Employee asEmployee = employeeRepository.findByPublicIdAndTenantId(teacher.getPublicId(), tenant.getId())
				.orElseThrow();

		assertInstanceOf(Teacher.class, asEmployee);
		assertEquals(teacher.getId(), asEmployee.getId());
		assertEquals(StaffCategory.TEACHING, asEmployee.getStaffCategory());
	}

	@Test
	void aNonTeachingEmployeeIsNotATeacher() {
		Employee driver = hireDriver();

		assertTrue(employeeRepository.findByPublicIdAndTenantId(driver.getPublicId(), tenant.getId()).isPresent());
		assertTrue(teacherRepository.findByPublicIdAndTenantId(driver.getPublicId(), tenant.getId()).isEmpty());
		assertFalse(teacherRepository.existsByIdAndTenantId(driver.getId(), tenant.getId()));
	}

	@Test
	void employeeCodesAreUniqueAcrossTeachingAndNonTeachingStaff() {
		Teacher teacher = hireTeacher();

		assertThrows(BusinessException.class, () -> employeeService.hire(StaffCategory.SUPPORT,
				teacher.getEmployeeCode(), "Dup", "Licate", "dup@school.test", LocalDate.of(2024, 1, 1)));
	}

	@Test
	void nonTeachingStaffGetSalaryAndPayslips() {
		Employee driver = hireDriver();
		salaryStructureService.create(driver.getPublicId().toString(),
				Map.of("BASIC", BigDecimal.valueOf(30000)), LocalDate.of(2026, 1, 1));

		Payslip payslip = payslipService.generate(driver.getId(), YearMonth.of(2026, 5));

		assertEquals(driver.getId(), payslip.getEmployeeId());
	}

	@Test
	void onlyAnActiveTeacherCanBeGivenAClass() {
		Employee driver = hireDriver();
		Teacher teacher = hireTeacher();
		String year = academicYearService.create("2026-27", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 3, 31), true)
				.getPublicId().toString();

		assertThrows(ResourceNotFoundException.class,
				() -> classroomService.create("CLS-D", "Grade 1", "A", year, driver.getId()),
				"a driver is not a teacher");
		assertEquals(teacher.getId(),
				classroomService.create("CLS-T", "Grade 1", "B", year, teacher.getId()).getClassTeacherId());
	}

	@Test
	void aClassTeacherCannotLeaveUntilTheClassIsReassigned() {
		Teacher teacher = hireTeacher();
		String year = academicYearService.create("2027-28", LocalDate.of(2027, 6, 1), LocalDate.of(2028, 3, 31), true)
				.getPublicId().toString();
		classroomService.create("CLS-X", "Grade 2", "A", year, teacher.getId());

		assertThrows(BusinessException.class, () -> employeeService.exit(teacher.getPublicId().toString(),
				EmployeeStatus.RESIGNED, LocalDate.now(), "Leaving"));
	}

	@Test
	void aLeaverIsExcludedFromCurrentStaffButStillOwedAFinalPayslip() {
		Employee driver = hireDriver();

		employeeService.exit(driver.getPublicId().toString(), EmployeeStatus.RETIRED, LocalDate.now().minusDays(5),
				"Retired");

		List<Employee> active = employeeRepository.findAllByTenantIdAndStatus(tenant.getId(), EmployeeStatus.ACTIVE);
		assertFalse(active.stream().anyMatch(e -> e.getId().equals(driver.getId())));
		List<Employee> payable = employeeRepository.findAllEmployedSince(tenant.getId(),
				LocalDate.now().minusDays(10));
		assertTrue(payable.stream().anyMatch(e -> e.getId().equals(driver.getId())));
		List<Employee> longGone = employeeRepository.findAllEmployedSince(tenant.getId(), LocalDate.now());
		assertFalse(longGone.stream().anyMatch(e -> e.getId().equals(driver.getId())));
	}

	@Test
	void softDeletingATeacherHidesItFromBothViews() {
		Teacher teacher = hireTeacher();
		Employee loaded = employeeRepository.findById(teacher.getId()).orElseThrow();

		loaded.softDelete("test");
		employeeRepository.saveAndFlush(loaded);

		assertTrue(employeeRepository.findByPublicIdAndTenantId(teacher.getPublicId(), tenant.getId()).isEmpty());
		assertTrue(teacherRepository.findByPublicIdAndTenantId(teacher.getPublicId(), tenant.getId()).isEmpty());
	}
}
