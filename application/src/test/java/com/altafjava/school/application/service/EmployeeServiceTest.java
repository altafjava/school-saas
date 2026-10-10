package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.employee.EmployeeCodeAllocator;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.common.model.Address;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.EmploymentType;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.teacher.model.Teacher;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	private static final UUID PUBLIC_ID = UUID.randomUUID();

	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private DepartmentRepository departmentRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private NumberSequenceService numberSequenceService;

	private EmployeeService service;

	@BeforeEach
	void setUp() {
		service = new EmployeeService(employeeRepository, departmentRepository, classroomRepository,
				new EmployeeCodeAllocator(employeeRepository, numberSequenceService));
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Employee clerk() {
		Employee employee = Employee.create(StaffCategory.ADMINISTRATIVE, "EMP-9", "Cara", "Clerk",
				"cara@school.test", LocalDate.of(2020, 1, 1));
		employee.setId(9L);
		employee.setTenantId(1L);
		when(employeeRepository.findByPublicIdAndTenantId(PUBLIC_ID, 1L)).thenReturn(Optional.of(employee));
		lenient().when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
		return employee;
	}

	@Test
	void hire_createsANonTeachingActiveEmployee() {
		when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

		Employee employee = service.hire(StaffCategory.SUPPORT, "EMP-5", "Sam", "Driver", "sam@school.test",
				LocalDate.of(2024, 3, 1));

		assertEquals(StaffCategory.SUPPORT, employee.getStaffCategory());
		assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
	}

	@Test
	void hire_aTeachingEmployeeThroughTheEmployeeApi_isRejected() {
		assertThrows(BusinessException.class, () -> service.hire(StaffCategory.TEACHING, "EMP-5", "Sam", "Lee",
				"sam@school.test", LocalDate.of(2024, 3, 1)));

		verify(employeeRepository, never()).save(any());
	}

	@Test
	void updateContactDetails_replacesMutableFields() {
		clerk();

		Employee updated = assertDoesNotThrow(
				() -> service.updateContactDetails(PUBLIC_ID.toString(), "Clara", "Clerk", "clara@school.test",
						ExpectedVersion.any()));

		assertEquals("Clara", updated.getFirstName());
		assertEquals("clara@school.test", updated.getEmail());
	}

	@Test
	void updateHrDetails_resolvesTheDepartmentAndSetsTheDesignation() {
		clerk();
		UUID departmentPublicId = UUID.randomUUID();
		Department department = Department.create("Admin", "ADM", null);
		department.setId(42L);
		when(departmentRepository.findByPublicIdAndTenantId(departmentPublicId, 1L))
				.thenReturn(Optional.of(department));

		Employee updated = service.updateHrDetails(PUBLIC_ID.toString(), departmentPublicId.toString(),
				"Office Manager", "B.Com", EmploymentType.FULL_TIME, ExpectedVersion.any());

		assertEquals(42L, updated.getDepartmentId());
		assertEquals("Office Manager", updated.getDesignation());
	}

	@Test
	void updateHrDetails_withoutDepartment_leavesItNull() {
		clerk();

		Employee updated = service.updateHrDetails(PUBLIC_ID.toString(), null, "Clerk", null, EmploymentType.PART_TIME,
				ExpectedVersion.any());

		assertNull(updated.getDepartmentId());
	}

	@Test
	void updatePhone_validatesTheNumber() {
		clerk();

		assertEquals("+14155552671",
				service.updatePhone(PUBLIC_ID.toString(), "+14155552671", ExpectedVersion.any()).getPhone());
		assertThrows(BusinessException.class,
				() -> service.updatePhone(PUBLIC_ID.toString(), "not-a-phone", ExpectedVersion.any()));
	}

	@Test
	void updateAddress_setsTheStructuredAddress() {
		clerk();
		Address address = Address.builder().line1("1 Rue de Rivoli").locality("Paris").postalCode("75001")
				.countryCode("FR").build();

		assertEquals("Paris",
				service.updateAddress(PUBLIC_ID.toString(), address, ExpectedVersion.any()).getAddress().getLocality());
	}

	@Test
	void exit_recordsTheLeaverAndTheReason() {
		clerk();

		Employee left = service.exit(PUBLIC_ID.toString(), EmployeeStatus.RESIGNED, LocalDate.now(), "Relocating");

		assertEquals(EmployeeStatus.RESIGNED, left.getStatus());
		assertEquals("Relocating", left.getExitReason());
	}

	@Test
	void exit_ofAClassTeacher_isBlockedUntilTheClassIsReassigned() {
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1));
		teacher.setId(3L);
		teacher.setTenantId(1L);
		when(employeeRepository.findByPublicIdAndTenantId(PUBLIC_ID, 1L)).thenReturn(Optional.of(teacher));
		when(classroomRepository.existsByClassTeacherIdAndTenantId(3L, 1L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> service.exit(PUBLIC_ID.toString(), EmployeeStatus.RESIGNED, LocalDate.now(), "x"));

		verify(employeeRepository, never()).save(any());
	}

	@Test
	void afterLeaving_theRecordCanNoLongerBeEdited() {
		Employee employee = clerk();
		employee.exit(EmployeeStatus.RETIRED, LocalDate.now(), null);

		assertThrows(BusinessException.class,
				() -> service.updateContactDetails(PUBLIC_ID.toString(), "A", "B", "a@b.test", ExpectedVersion.any()));
		assertThrows(BusinessException.class,
				() -> service.updatePhone(PUBLIC_ID.toString(), "+14155552671", ExpectedVersion.any()));
	}

	@Test
	void findByPublicId_missing_throwsResourceNotFound() {
		when(employeeRepository.findByPublicIdAndTenantId(PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.findByPublicId(PUBLIC_ID.toString()));
	}
}
