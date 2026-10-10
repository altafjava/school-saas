package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import com.altafjava.school.domain.payroll.model.PayComponentType;
import com.altafjava.school.domain.payroll.model.PayrollComputation;
import com.altafjava.school.domain.payroll.model.Payslip;
import com.altafjava.school.domain.payroll.model.PayslipStatus;
import com.altafjava.school.domain.payroll.model.SalaryStructure;
import com.altafjava.school.domain.payroll.repository.PayslipRepository;
import com.altafjava.school.domain.payroll.repository.SalaryStructureRepository;

@ExtendWith(MockitoExtension.class)
class PayslipServiceTest {

	@Mock
	private PayslipRepository payslipRepository;
	@Mock
	private SalaryStructureRepository salaryStructureRepository;
	@Mock
	private LeaveRequestRepository leaveRequestRepository;
	@Mock
	private LeaveTypeRepository leaveTypeRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private PublicIdLookup publicIdLookup;

	private PayslipService payslipService;

	@BeforeEach
	void setUp() {
		payslipService = new PayslipService(payslipRepository, salaryStructureRepository, leaveRequestRepository,
				leaveTypeRepository, employeeRepository, publicIdLookup);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private SalaryStructure activeStructure() {
		List<PayComponentAmount> components = List.of(
				new PayComponentAmount("BASIC", "Basic Pay", PayComponentType.EARNING, BigDecimal.valueOf(50000)),
				new PayComponentAmount("HRA", "House Rent Allowance", PayComponentType.EARNING,
						BigDecimal.valueOf(10000)),
				new PayComponentAmount("TRANSPORT", "Transport Allowance", PayComponentType.EARNING,
						BigDecimal.valueOf(2000)),
				new PayComponentAmount("OTHER_ALLOWANCE", "Other Allowances", PayComponentType.EARNING,
						BigDecimal.valueOf(500)),
				new PayComponentAmount("OTHER_DEDUCTION", "Other Deductions", PayComponentType.DEDUCTION,
						BigDecimal.valueOf(1000)));
		return SalaryStructure.create(10L, components, LocalDate.of(2026, 1, 1));
	}

	@Test
	void generate_withActiveStructureAndNoUnpaidLeave_savesDraftPayslip() {
		YearMonth payMonth = YearMonth.of(2026, 6);
		when(payslipRepository.existsByEmployeeIdAndPayYearAndPayMonthAndTenantId(10L, 2026, 6, 1L))
				.thenReturn(false);
		when(salaryStructureRepository.findByEmployeeIdAndActiveTrueAndTenantId(10L, 1L))
				.thenReturn(Optional.of(activeStructure()));
		when(leaveTypeRepository.findAllByTenantIdAndPaidFalse(1L)).thenReturn(List.of());
		when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

		Payslip payslip = payslipService.generate(10L, payMonth);

		assertEquals(PayslipStatus.DRAFT, payslip.getStatus());
		assertEquals(0, BigDecimal.valueOf(62500).compareTo(payslip.getGrossPay()));
		assertEquals(0, BigDecimal.ZERO.compareTo(payslip.getLossOfPayDays()));
		verify(leaveRequestRepository, never()).findOverlappingByEmployeeIdAndStatusAndLeaveTypeIdIn(any(), any(),
				any(), any(), any(), any());
	}

	@Test
	void generate_withUnpaidLeaveType_queriesOverlappingApprovedLeave() {
		YearMonth payMonth = YearMonth.of(2026, 6);
		LeaveType unpaidType = LeaveType.create("Unpaid Leave", BigDecimal.ZERO);
		unpaidType.setId(99L);
		unpaidType.markUnpaid();
		when(payslipRepository.existsByEmployeeIdAndPayYearAndPayMonthAndTenantId(10L, 2026, 6, 1L))
				.thenReturn(false);
		when(salaryStructureRepository.findByEmployeeIdAndActiveTrueAndTenantId(10L, 1L))
				.thenReturn(Optional.of(activeStructure()));
		when(leaveTypeRepository.findAllByTenantIdAndPaidFalse(1L)).thenReturn(List.of(unpaidType));
		when(leaveRequestRepository.findOverlappingByEmployeeIdAndStatusAndLeaveTypeIdIn(10L, 1L,
				LeaveRequestStatus.APPROVED, List.of(99L), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
				.thenReturn(List.of());
		when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

		payslipService.generate(10L, payMonth);

		verify(leaveRequestRepository).findOverlappingByEmployeeIdAndStatusAndLeaveTypeIdIn(eq(10L), eq(1L),
				eq(LeaveRequestStatus.APPROVED), eq(List.of(99L)), eq(LocalDate.of(2026, 6, 1)),
				eq(LocalDate.of(2026, 6, 30)));
	}

	@Test
	void generate_withoutActiveSalaryStructure_throwsBusinessException() {
		when(payslipRepository.existsByEmployeeIdAndPayYearAndPayMonthAndTenantId(10L, 2026, 6, 1L))
				.thenReturn(false);
		when(salaryStructureRepository.findByEmployeeIdAndActiveTrueAndTenantId(10L, 1L)).thenReturn(Optional.empty());

		assertThrows(BusinessException.class, () -> payslipService.generate(10L, YearMonth.of(2026, 6)));
	}

	@Test
	void generate_whenPayslipAlreadyExists_throwsBusinessException() {
		when(payslipRepository.existsByEmployeeIdAndPayYearAndPayMonthAndTenantId(10L, 2026, 6, 1L))
				.thenReturn(true);

		assertThrows(BusinessException.class, () -> payslipService.generate(10L, YearMonth.of(2026, 6)));
	}

	@Test
	void finalizePayslip_resolvesAndTransitionsPayslip() {
		Payslip payslip = Payslip.generate(10L, 2026, 6,
				activeStructure().toSnapshot(), payrollComputation());
		UUID publicId = UUID.randomUUID();
		payslip.setPublicId(publicId);
		when(payslipRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(payslip));
		when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

		Payslip result = payslipService.finalizePayslip(publicId.toString());

		assertEquals(PayslipStatus.FINALIZED, result.getStatus());
	}

	@Test
	void findByPublicId_whenNotFound_throwsResourceNotFoundException() {
		UUID publicId = UUID.randomUUID();
		when(payslipRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> payslipService.findByPublicId(publicId.toString()));
	}

	private PayrollComputation payrollComputation() {
		return new PayrollComputation(BigDecimal.valueOf(62500), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.valueOf(61500));
	}
}
