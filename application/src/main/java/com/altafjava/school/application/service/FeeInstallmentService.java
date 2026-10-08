package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.fee.model.FeeInstallment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeInstallmentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.fee.service.InstallmentPlanFactory;
import com.altafjava.school.domain.fee.service.InstallmentPlanFactory.Slot;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Manages a student's installment plan for a fee structure. Plans are stored as shares of the net
 * fee, so changing a plan never changes what is owed — only when. Setting a plan replaces the
 * previous one (kept as soft-deleted history); how payments fall across it is computed on read.
 */
@Service
@RequiredArgsConstructor
public class FeeInstallmentService {

	private final FeeInstallmentRepository feeInstallmentRepository;
	private final StudentRepository studentRepository;
	private final FeeStructureRepository feeStructureRepository;

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "FeeInstallmentPlan", details = "Installment plan set")
	public List<FeeInstallment> setEvenPlan(String studentPublicId, String feeStructurePublicId, int count,
			LocalDate firstDueDate, int intervalMonths) {
		Context context = context(studentPublicId, feeStructurePublicId);
		return replacePlan(context, InstallmentPlanFactory.evenSplit(context.student().getId(),
				context.structure().getId(), count, firstDueDate, intervalMonths));
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "FeeInstallmentPlan", details = "Installment plan set")
	public List<FeeInstallment> setCustomPlan(String studentPublicId, String feeStructurePublicId,
			List<Slot> slots) {
		Context context = context(studentPublicId, feeStructurePublicId);
		return replacePlan(context, InstallmentPlanFactory.fromSlots(context.student().getId(),
				context.structure().getId(), slots));
	}

	@Transactional
	@Audited(action = AuditAction.DELETE, resourceType = "FeeInstallmentPlan", details = "Installment plan removed")
	public void removePlan(String studentPublicId, String feeStructurePublicId) {
		Context context = context(studentPublicId, feeStructurePublicId);
		replacePlan(context, List.of());
	}

	@Transactional(readOnly = true)
	public List<FeeInstallment> getPlan(String studentPublicId, String feeStructurePublicId) {
		Context context = context(studentPublicId, feeStructurePublicId);
		return feeInstallmentRepository.findByStudentAndStructure(context.tenantId(), context.student().getId(),
				context.structure().getId());
	}

	private List<FeeInstallment> replacePlan(Context context, List<FeeInstallment> newPlan) {
		feeInstallmentRepository.findByStudentAndStructure(context.tenantId(), context.student().getId(),
				context.structure().getId()).forEach(old -> {
					old.softDelete("installment-plan-replaced");
					feeInstallmentRepository.save(old);
				});
		// Hibernate runs inserts before updates; flush the soft-deletes first so the new rows do not
		// collide with the old ones on the unique (student, structure, position) index.
		feeInstallmentRepository.flush();
		return feeInstallmentRepository.saveAll(newPlan);
	}

	private Context context(String studentPublicId, String feeStructurePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		FeeStructure structure = feeStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(feeStructurePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Fee structure not found: " + feeStructurePublicId));
		return new Context(tenantId, student, structure);
	}

	private record Context(Long tenantId, Student student, FeeStructure structure) {
	}
}
