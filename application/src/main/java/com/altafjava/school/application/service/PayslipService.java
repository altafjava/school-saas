package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;
import com.altafjava.school.domain.payroll.model.PayrollComputation;
import com.altafjava.school.domain.payroll.model.Payslip;
import com.altafjava.school.domain.payroll.model.SalaryStructure;
import com.altafjava.school.domain.payroll.repository.PayslipRepository;
import com.altafjava.school.domain.payroll.repository.SalaryStructureRepository;
import com.altafjava.school.domain.payroll.service.PayrollCalculator;

@Service
public class PayslipService {

	private final PayslipRepository payslipRepository;
	private final SalaryStructureRepository salaryStructureRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final LeaveTypeRepository leaveTypeRepository;
	private final EmployeeRepository employeeRepository;
	// Pure domain logic, no Spring wiring — instantiated directly, mirroring how FeePaymentService
	// holds its FeeBalanceCalculator.
	private final PayrollCalculator payrollCalculator = new PayrollCalculator();

	public PayslipService(PayslipRepository payslipRepository, SalaryStructureRepository salaryStructureRepository,
			LeaveRequestRepository leaveRequestRepository, LeaveTypeRepository leaveTypeRepository,
			EmployeeRepository employeeRepository) {
		this.payslipRepository = payslipRepository;
		this.salaryStructureRepository = salaryStructureRepository;
		this.leaveRequestRepository = leaveRequestRepository;
		this.leaveTypeRepository = leaveTypeRepository;
		this.employeeRepository = employeeRepository;
	}

	@Transactional(readOnly = true)
	public Page<Payslip> listAll(Pageable pageable) {
		return payslipRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<Payslip> listForEmployee(String employeePublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = employeeRepository.findByPublicIdAndTenantId(UUID.fromString(employeePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeePublicId));
		return payslipRepository.findAllByEmployeeIdAndTenantId(employee.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public Payslip findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return payslipRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Payslip not found: " + publicId));
	}

	/**
	 * Generates one draft payslip for the given employee and month, using the employee's currently
	 * active {@link SalaryStructure} and their approved unpaid-leave days that month. Called by
	 * {@code PayslipGenerationJob}; employeeId is a resolved entity id rather than a public id since
	 * the job already holds {@link Employee} entities from a tenant-wide scan.
	 */
	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "Payslip", details = "Payslip generated")
	public Payslip generate(Long employeeId, YearMonth payMonth) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (payslipRepository.existsByEmployeeIdAndPayYearAndPayMonthAndTenantId(employeeId, payMonth.getYear(),
				payMonth.getMonthValue(), tenantId)) {
			throw new BusinessException(
					"Payslip already exists for employee " + employeeId + " for " + payMonth);
		}
		SalaryStructure structure = salaryStructureRepository.findByEmployeeIdAndActiveTrueAndTenantId(employeeId,
				tenantId).orElseThrow(
						() -> new BusinessException("No active salary structure for employee " + employeeId));

		List<LeaveRequest> unpaidApprovedLeave = findUnpaidApprovedLeaveInMonth(tenantId, employeeId, payMonth);
		PayrollComputation computation = payrollCalculator.compute(structure.toSnapshot(), payMonth,
				unpaidApprovedLeave);
		Payslip payslip = Payslip.generate(employeeId, payMonth.getYear(), payMonth.getMonthValue(),
				structure.toSnapshot(), computation);
		return payslipRepository.save(payslip);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Payslip", details = "Payslip finalized")
	public Payslip finalizePayslip(String publicId) {
		Payslip payslip = findByPublicId(publicId);
		payslip.finalizePayslip();
		return payslipRepository.save(payslip);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Payslip", details = "Payslip marked disbursed")
	public Payslip markDisbursed(String publicId) {
		Payslip payslip = findByPublicId(publicId);
		payslip.markDisbursed();
		return payslipRepository.save(payslip);
	}

	private List<LeaveRequest> findUnpaidApprovedLeaveInMonth(Long tenantId, Long employeeId, YearMonth payMonth) {
		List<Long> unpaidLeaveTypeIds = leaveTypeRepository.findAllByTenantIdAndPaidFalse(tenantId).stream()
				.map(LeaveType::getId)
				.toList();
		if (unpaidLeaveTypeIds.isEmpty()) {
			return List.of();
		}
		LocalDate monthStart = payMonth.atDay(1);
		LocalDate monthEnd = payMonth.atEndOfMonth();
		return leaveRequestRepository.findOverlappingByEmployeeIdAndStatusAndLeaveTypeIdIn(employeeId, tenantId,
				LeaveRequestStatus.APPROVED, unpaidLeaveTypeIds, monthStart, monthEnd);
	}
}
