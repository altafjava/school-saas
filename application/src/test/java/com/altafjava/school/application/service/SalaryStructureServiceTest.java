package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import com.altafjava.school.domain.payroll.model.PayComponentDefinition;
import com.altafjava.school.domain.payroll.model.PayComponentType;
import com.altafjava.school.domain.payroll.model.SalaryStructure;
import com.altafjava.school.domain.payroll.repository.PayComponentDefinitionRepository;
import com.altafjava.school.domain.payroll.repository.SalaryStructureRepository;

@ExtendWith(MockitoExtension.class)
class SalaryStructureServiceTest {

	@Mock
	private SalaryStructureRepository salaryStructureRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private PayComponentDefinitionRepository payComponentDefinitionRepository;

	private SalaryStructureService salaryStructureService;

	@BeforeEach
	void setUp() {
		salaryStructureService = new SalaryStructureService(salaryStructureRepository, employeeRepository,
				payComponentDefinitionRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Employee employeeWithPublicId(UUID publicId, long id) {
		Employee employee = Employee.create(StaffCategory.SUPPORT, "EMP-1", "Jane", "Doe", "jane@school.test",
				LocalDate.of(2020, 1, 1));
		employee.setId(id);
		employee.setPublicId(publicId);
		return employee;
	}

	private PayComponentDefinition basicPayDefinition() {
		return PayComponentDefinition.create("BASIC", "Basic Pay", PayComponentType.EARNING, 1);
	}

	private void stubBasicPayDefinition() {
		when(payComponentDefinitionRepository.findByCodeAndTenantId("BASIC", 1L))
				.thenReturn(Optional.of(basicPayDefinition()));
	}

	private List<PayComponentAmount> existingComponents(BigDecimal basicPay) {
		return List.of(new PayComponentAmount("BASIC", "Basic Pay", PayComponentType.EARNING, basicPay));
	}

	@Test
	void create_withNoExistingActiveStructure_savesNewActiveStructure() {
		UUID employeePublicId = UUID.randomUUID();
		Employee employee = employeeWithPublicId(employeePublicId, 10L);
		when(employeeRepository.findByPublicIdAndTenantId(employeePublicId, 1L)).thenReturn(Optional.of(employee));
		when(salaryStructureRepository.findByEmployeeIdAndActiveTrueAndTenantId(10L, 1L)).thenReturn(Optional.empty());
		when(salaryStructureRepository.save(any(SalaryStructure.class))).thenAnswer(inv -> inv.getArgument(0));
		stubBasicPayDefinition();

		SalaryStructure result = salaryStructureService.create(employeePublicId.toString(),
				Map.of("BASIC", BigDecimal.valueOf(50000)), LocalDate.of(2026, 1, 1));

		assertTrue(result.isActive());
		assertEquals(10L, result.getEmployeeId());
		verify(salaryStructureRepository, times(1)).save(any(SalaryStructure.class));
	}

	@Test
	void create_withExistingActiveStructure_deactivatesPreviousBeforeSavingNew() {
		UUID employeePublicId = UUID.randomUUID();
		Employee employee = employeeWithPublicId(employeePublicId, 10L);
		SalaryStructure existing = SalaryStructure.create(10L, existingComponents(BigDecimal.valueOf(40000)),
				LocalDate.of(2025, 1, 1));
		when(employeeRepository.findByPublicIdAndTenantId(employeePublicId, 1L)).thenReturn(Optional.of(employee));
		when(salaryStructureRepository.findByEmployeeIdAndActiveTrueAndTenantId(10L, 1L))
				.thenReturn(Optional.of(existing));
		when(salaryStructureRepository.save(any(SalaryStructure.class))).thenAnswer(inv -> inv.getArgument(0));
		stubBasicPayDefinition();

		salaryStructureService.create(employeePublicId.toString(), Map.of("BASIC", BigDecimal.valueOf(60000)),
				LocalDate.of(2026, 1, 1));

		assertEquals(false, existing.isActive());
		verify(salaryStructureRepository, times(2)).save(any(SalaryStructure.class));
	}

	@Test
	void create_withUnknownEmployee_throwsResourceNotFoundException() {
		UUID employeePublicId = UUID.randomUUID();
		when(employeeRepository.findByPublicIdAndTenantId(employeePublicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> salaryStructureService.create(employeePublicId.toString(),
						Map.of("BASIC", BigDecimal.valueOf(50000)),
						LocalDate.of(2026, 1, 1)));
	}

	@Test
	void create_withUnknownComponentCode_throwsBusinessException() {
		UUID employeePublicId = UUID.randomUUID();
		Employee employee = employeeWithPublicId(employeePublicId, 10L);
		when(employeeRepository.findByPublicIdAndTenantId(employeePublicId, 1L)).thenReturn(Optional.of(employee));
		when(payComponentDefinitionRepository.findByCodeAndTenantId("GHOST", 1L)).thenReturn(Optional.empty());

		assertThrows(BusinessException.class,
				() -> salaryStructureService.create(employeePublicId.toString(),
						Map.of("GHOST", BigDecimal.valueOf(50000)),
						LocalDate.of(2026, 1, 1)));
	}

	@Test
	void create_withInactiveComponentCode_throwsBusinessException() {
		UUID employeePublicId = UUID.randomUUID();
		Employee employee = employeeWithPublicId(employeePublicId, 10L);
		PayComponentDefinition inactive = basicPayDefinition();
		inactive.setActive(false);
		when(employeeRepository.findByPublicIdAndTenantId(employeePublicId, 1L)).thenReturn(Optional.of(employee));
		when(payComponentDefinitionRepository.findByCodeAndTenantId("BASIC", 1L)).thenReturn(Optional.of(inactive));

		assertThrows(BusinessException.class,
				() -> salaryStructureService.create(employeePublicId.toString(),
						Map.of("BASIC", BigDecimal.valueOf(50000)),
						LocalDate.of(2026, 1, 1)));
	}

	@Test
	void supersede_resolvesEmployeeFromCurrentStructureAndDeactivatesIt() {
		UUID currentPublicId = UUID.randomUUID();
		SalaryStructure current = SalaryStructure.create(10L, existingComponents(BigDecimal.valueOf(40000)),
				LocalDate.of(2025, 1, 1));
		current.setPublicId(currentPublicId);
		when(salaryStructureRepository.findByPublicIdAndTenantId(currentPublicId, 1L)).thenReturn(Optional.of(current));
		when(salaryStructureRepository.findByEmployeeIdAndActiveTrueAndTenantId(10L, 1L))
				.thenReturn(Optional.of(current));
		when(salaryStructureRepository.save(any(SalaryStructure.class))).thenAnswer(inv -> inv.getArgument(0));
		stubBasicPayDefinition();

		SalaryStructure result = salaryStructureService.supersede(currentPublicId.toString(),
				Map.of("BASIC", BigDecimal.valueOf(60000)), LocalDate.of(2026, 1, 1));

		assertEquals(10L, result.getEmployeeId());
		assertEquals(false, current.isActive());
	}
}
