package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.school.application.employee.EmployeeCodeAllocator;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private NumberSequenceService numberSequenceService;

	private TeacherService teacherService;

	@BeforeEach
	void setUp() {
		teacherService = new TeacherService(teacherRepository,
				new EmployeeCodeAllocator(employeeRepository, numberSequenceService));
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void hire_createsAnActiveTeachingEmployee() {
		when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

		Teacher teacher = teacherService.hire("EMP-1", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1));

		assertEquals("Jane", teacher.getFirstName());
		assertEquals(StaffCategory.TEACHING, teacher.getStaffCategory());
		assertEquals(EmployeeStatus.ACTIVE, teacher.getStatus());
	}

	@Test
	void hire_withoutEmployeeCode_generatesOneFromTheSharedSequence() {
		when(numberSequenceService.generateNext(1L, "EMPLOYEE_CODE", "EMP-", 4, ResetPeriod.NEVER))
				.thenReturn("EMP-0003");
		when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

		Teacher teacher = teacherService.hire(null, "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1));

		assertEquals("EMP-0003", teacher.getEmployeeCode());
	}

	@Test
	void hire_codeUsedByAnyEmployee_isRejected() {
		when(employeeRepository.existsByEmployeeCodeAndTenantId("EMP-1", 1L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> teacherService.hire("EMP-1", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1)));
	}

	@Test
	void findByPublicId_missing_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		when(teacherRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> teacherService.findByPublicId(publicId.toString()));
	}
}
