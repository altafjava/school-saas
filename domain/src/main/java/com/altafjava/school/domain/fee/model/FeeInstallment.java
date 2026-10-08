package com.altafjava.school.domain.fee.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One due date in a student's installment plan for a fee structure. It stores a <em>share</em> of
 * the net fee (in basis points, 10 000 = 100 %), not a currency amount: the amount is derived from
 * the fee as it stands, so a later discount or fee revision re-spreads over the plan instead of
 * leaving stale amounts. The shares of a plan always total {@link #TOTAL_BASIS_POINTS}.
 */
@Entity
@Table(name = "fee_installments")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class FeeInstallment extends SoftDeletableEntity {

	public static final int TOTAL_BASIS_POINTS = 10_000;

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to fee_structures.id
	@Column(name = "fee_structure_id", nullable = false)
	private Long feeStructureId;

	@Column(name = "sequence_number", nullable = false)
	private int sequenceNumber;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "share_basis_points", nullable = false)
	private int shareBasisPoints;

	public static FeeInstallment of(Long studentId, Long feeStructureId, int sequenceNumber, LocalDate dueDate,
			int shareBasisPoints) {
		if (shareBasisPoints < 1 || shareBasisPoints > TOTAL_BASIS_POINTS) {
			throw new BusinessException("Installment share must be between 0.01% and 100%");
		}
		return FeeInstallment.builder()
				.studentId(studentId)
				.feeStructureId(feeStructureId)
				.sequenceNumber(sequenceNumber)
				.dueDate(dueDate)
				.shareBasisPoints(shareBasisPoints)
				.build();
	}
}
