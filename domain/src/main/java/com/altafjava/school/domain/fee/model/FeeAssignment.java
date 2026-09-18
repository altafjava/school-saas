package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fee_assignments")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class FeeAssignment extends SoftDeletableEntity {

	// FK to fee_structures.id
	@Column(name = "fee_structure_id", nullable = false)
	private Long feeStructureId;

	@Enumerated(EnumType.STRING)
	@Column(name = "scope", nullable = false, length = 20)
	private FeeAssignmentScope scope;

	// FK to students.id — set iff scope == STUDENT
	@Column(name = "student_id")
	private Long studentId;

	// FK to classrooms.id — set iff scope == CLASSROOM
	@Column(name = "classroom_id")
	private Long classroomId;

	// Nullable — no due date means no late fee is ever applied for this assignment, regardless of
	// the owning FeeStructure's late-fee policy (see FeeBalanceCalculator).
	@Column(name = "due_date")
	private LocalDate dueDate;

	// Per-assignment overrides of the owning FeeStructure's grace-days/late-fee-percentage
	// defaults — null defers to the structure's value, which itself may defer to a hardcoded
	// system default (0 grace days, 0% late fee). See FeeBalanceCalculator.
	@Column(name = "grace_days")
	private Integer graceDays;

	@Column(name = "late_fee_percentage", precision = 5, scale = 2)
	private BigDecimal lateFeePercentage;

	public static FeeAssignment forStudent(Long feeStructureId, Long studentId) {
		return FeeAssignment.builder()
				.feeStructureId(feeStructureId)
				.scope(FeeAssignmentScope.STUDENT)
				.studentId(studentId)
				.build();
	}

	public static FeeAssignment forClassroom(Long feeStructureId, Long classroomId) {
		return FeeAssignment.builder()
				.feeStructureId(feeStructureId)
				.scope(FeeAssignmentScope.CLASSROOM)
				.classroomId(classroomId)
				.build();
	}

	public void configureDueDate(LocalDate dueDate, Integer graceDays, BigDecimal lateFeePercentage) {
		this.dueDate = dueDate;
		this.graceDays = graceDays;
		this.lateFeePercentage = lateFeePercentage;
	}

	/**
	 * Enforced at persist time rather than only in {@link #forStudent}/{@link #forClassroom} so a
	 * mismatched {@code scope}/FK combination can never reach the database regardless of how the
	 * entity was constructed (e.g. directly via the builder).
	 */
	@PrePersist
	@PreUpdate
	private void validateScope() {
		boolean hasStudent = studentId != null;
		boolean hasClassroom = classroomId != null;
		if (scope == FeeAssignmentScope.STUDENT && (!hasStudent || hasClassroom)) {
			throw new BusinessException("STUDENT-scoped fee assignment must set studentId and not classroomId");
		}
		if (scope == FeeAssignmentScope.CLASSROOM && (!hasClassroom || hasStudent)) {
			throw new BusinessException("CLASSROOM-scoped fee assignment must set classroomId and not studentId");
		}
	}
}
