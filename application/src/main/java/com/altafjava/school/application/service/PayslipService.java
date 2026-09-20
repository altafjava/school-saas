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
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class PayslipService {

	private final PayslipRepository payslipRepository;
	private final SalaryStructureRepository salaryStructureRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final LeaveTypeRepository leaveTypeRepository;
	private final TeacherRepository teacherRepository;
	// Pure domain logic, no Spring wiring — instantiated directly, mirroring how FeePaymentService
	// holds its FeeBalanceCalculator.
	private final PayrollCalculator payrollCalculator = new PayrollCalculator();

	public PayslipService(PayslipRepository payslipRepository, SalaryStructureRepository salaryStructureRepository,
			LeaveRequestRepository leaveRequestRepository, LeaveTypeRepository leaveTypeRepository,
			TeacherRepository teacherRepository) {
		this.payslipRepository = payslipRepository;
		this.salaryStructureRepository = salaryStructureRepository;
		this.leaveRequestRepository = leaveRequestRepository;
		this.leaveTypeRepository = leaveTypeRepository;
		this.teacherRepository = teacherRepository;
	}

	@Transactional(readOnly = true)
	public Page<Payslip> listAll(Pageable pageable) {
		return payslipRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<Payslip> listForTeacher(String teacherPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher teacher = teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherPublicId));
		return payslipRepository.findAllByTeacherIdAndTenantId(teacher.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public Payslip findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return payslipRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Payslip not found: " + publicId));
	}

	/**
	 * Generates one draft payslip for the given teacher and month, using the teacher's currently
	 * active {@link SalaryStructure} and their approved unpaid-leave days that month. Called by
	 * {@code PayslipGenerationJob}; teacherId is a resolved entity id rather than a public id since
	 * the job already holds {@link Teacher} entities from a tenant-wide scan.
	 */
	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "Payslip", details = "Payslip generated")
	public Payslip generate(Long teacherId, YearMonth payMonth) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (payslipRepository.existsByTeacherIdAndPayYearAndPayMonthAndTenantId(teacherId, payMonth.getYear(),
				payMonth.getMonthValue(), tenantId)) {
			throw new BusinessException(
					"Payslip already exists for teacher " + teacherId + " for " + payMonth);
		}
		SalaryStructure structure = salaryStructureRepository.findByTeacherIdAndActiveTrueAndTenantId(teacherId,
				tenantId).orElseThrow(
						() -> new BusinessException("No active salary structure for teacher " + teacherId));

		List<LeaveRequest> unpaidApprovedLeave = findUnpaidApprovedLeaveInMonth(tenantId, teacherId, payMonth);
		PayrollComputation computation = payrollCalculator.compute(structure.toSnapshot(), payMonth,
				unpaidApprovedLeave);
		Payslip payslip = Payslip.generate(teacherId, payMonth.getYear(), payMonth.getMonthValue(),
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

	private List<LeaveRequest> findUnpaidApprovedLeaveInMonth(Long tenantId, Long teacherId, YearMonth payMonth) {
		List<Long> unpaidLeaveTypeIds = leaveTypeRepository.findAllByTenantIdAndPaidFalse(tenantId).stream()
				.map(LeaveType::getId)
				.toList();
		if (unpaidLeaveTypeIds.isEmpty()) {
			return List.of();
		}
		LocalDate monthStart = payMonth.atDay(1);
		LocalDate monthEnd = payMonth.atEndOfMonth();
		return leaveRequestRepository.findOverlappingByTeacherIdAndStatusAndLeaveTypeIdIn(teacherId, tenantId,
				LeaveRequestStatus.APPROVED, unpaidLeaveTypeIds, monthStart, monthEnd);
	}
}
