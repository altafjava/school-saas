package com.altafjava.school.application.importing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeAssignmentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.importing.model.ImportType;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * What a student owed when the school moved onto the system, as a charge of its own under one
 * "Opening balance" fee. It is then paid, invoiced and reported like any other fee.
 */
@Component
@RequiredArgsConstructor
class OpeningFeeBalanceRowImporter implements RowImporter {

	static final String PLAN_TYPE = "OPENING_BALANCE";
	private static final String STRUCTURE_NAME = "Opening balance";

	private final StudentRepository studentRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final FeeAssignmentRepository feeAssignmentRepository;

	@Override
	public ImportType type() {
		return ImportType.OPENING_FEE_BALANCES;
	}

	@Override
	public String requiredPermission() {
		return SchoolPermissions.FEE_STRUCTURE_MANAGE;
	}

	@Override
	public List<ImportField> fields() {
		return List.of(ImportField.required("studentCode", "The student's admission number"),
				ImportField.required("amount", "Amount still owed, like 2500.00"),
				ImportField.optional("dueDate", "When it is due, like 2026-06-30; late fees need one"));
	}

	@Override
	public Optional<String> uniqueKey(ImportRow row) {
		return Optional.ofNullable(row.text("studentCode")).map(code -> code.toLowerCase(Locale.ROOT));
	}

	@Override
	public void apply(ImportRow row) {
		Long tenantId = TenantContext.getCurrentTenantId();
		String studentCode = row.requiredText("studentCode");
		Student student = studentRepository.findByStudentCodeAndTenantId(studentCode, tenantId)
				.orElseThrow(() -> new ImportRowException("studentCode", "No student has the code " + studentCode));
		BigDecimal amount = row.requiredAmount("amount");
		FeeStructure openingBalance = feeStructureRepository.findFirstByPlanTypeAndTenantIdOrderByIdAsc(PLAN_TYPE,
				tenantId).orElseGet(
						() -> feeStructureRepository
								.save(FeeStructure.create(STRUCTURE_NAME, BigDecimal.ZERO, FeeFrequency.ONE_TIME,
										PLAN_TYPE)));
		if (feeAssignmentRepository.existsByFeeStructureIdAndStudentIdAndTenantId(openingBalance.getId(),
				student.getId(), tenantId)) {
			throw new ImportRowException("studentCode", "Student " + studentCode + " already has an opening balance");
		}
		FeeAssignment charge = FeeAssignment.forStudent(openingBalance.getId(), student.getId());
		charge.chargeIndividually(amount);
		charge.configureDueDate(row.date("dueDate"), null, null);
		feeAssignmentRepository.save(charge);
	}
}
