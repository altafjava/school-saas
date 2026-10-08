package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A student's position on one fee structure. {@code amountDue} is net of discounts and
 * {@code amountPaid} net of completed refunds. With an installment plan, {@code dueDate} is the
 * next unpaid installment's date and {@code installments} lists every one; without, {@code dueDate}
 * is the assignment's.
 */
public record FeeBalance(
		Long feeStructureId,
		String feeStructureName,
		BigDecimal grossAmount,
		BigDecimal discountAmount,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		BigDecimal refundedAmount,
		BigDecimal outstandingBalance,
		BigDecimal overpaidAmount,
		BigDecimal lateFeeAmount,
		LocalDate dueDate,
		List<InstallmentStatus> installments) {

	public FeeBalance {
		installments = installments == null ? List.of() : List.copyOf(installments);
	}

	/** For callers with no discounts, refunds or installments: gross equals due, nothing refunded. */
	public FeeBalance(Long feeStructureId, String feeStructureName, BigDecimal amountDue, BigDecimal amountPaid,
			BigDecimal outstandingBalance, BigDecimal overpaidAmount, BigDecimal lateFeeAmount, LocalDate dueDate) {
		this(feeStructureId, feeStructureName, amountDue, BigDecimal.ZERO, amountDue, amountPaid, BigDecimal.ZERO,
				outstandingBalance, overpaidAmount, lateFeeAmount, dueDate, List.of());
	}
}
