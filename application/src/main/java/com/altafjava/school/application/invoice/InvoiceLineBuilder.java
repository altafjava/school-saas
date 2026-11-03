package com.altafjava.school.application.invoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeInvoiceLine;
import com.altafjava.school.domain.fee.model.FeeInvoiceLine.ChargeKey;
import com.altafjava.school.domain.fee.model.InstallmentStatus;
import com.altafjava.school.domain.fee.model.InvoicePeriod;

/**
 * Turns a student's fee balances into the charges one invoice bills: each installment falling due
 * in the period, or the whole outstanding fee when it has no installments. A charge already on a
 * live invoice is never billed again.
 */
final class InvoiceLineBuilder {

	private InvoiceLineBuilder() {
	}

	static List<FeeInvoiceLine> linesFor(Collection<FeeBalance> balances, InvoicePeriod period,
			Set<ChargeKey> alreadyBilled) {
		List<FeeInvoiceLine> lines = new ArrayList<>();
		for (FeeBalance balance : balances) {
			if (balance.installments().isEmpty()) {
				wholeFee(balance, period, alreadyBilled, lines);
			} else {
				installments(balance, period, alreadyBilled, lines);
			}
		}
		return lines;
	}

	private static void wholeFee(FeeBalance balance, InvoicePeriod period, Set<ChargeKey> alreadyBilled,
			List<FeeInvoiceLine> lines) {
		boolean due = balance.outstandingBalance().signum() > 0 && period.covers(balance.dueDate());
		if (due && !alreadyBilled.contains(new ChargeKey(balance.feeStructureId(), null))) {
			lines.add(new FeeInvoiceLine(balance.feeStructureId(), null, balance.feeStructureName(),
					balance.outstandingBalance()));
		}
	}

	private static void installments(FeeBalance balance, InvoicePeriod period, Set<ChargeKey> alreadyBilled,
			List<FeeInvoiceLine> lines) {
		for (InstallmentStatus installment : balance.installments()) {
			ChargeKey key = new ChargeKey(balance.feeStructureId(), installment.sequenceNumber());
			if (installment.outstandingAmount().signum() > 0 && period.covers(installment.dueDate())
					&& !alreadyBilled.contains(key)) {
				lines.add(new FeeInvoiceLine(balance.feeStructureId(), installment.sequenceNumber(),
						balance.feeStructureName() + " — installment " + installment.sequenceNumber(),
						installment.outstandingAmount()));
			}
		}
	}
}
