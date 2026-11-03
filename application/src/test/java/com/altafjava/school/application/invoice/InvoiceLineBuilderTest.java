package com.altafjava.school.application.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeInvoiceLine;
import com.altafjava.school.domain.fee.model.FeeInvoiceLine.ChargeKey;
import com.altafjava.school.domain.fee.model.InstallmentStatus;
import com.altafjava.school.domain.fee.model.InvoicePeriod;

class InvoiceLineBuilderTest {

	private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
	private static final InvoicePeriod NOVEMBER = new InvoicePeriod(NOV_1, LocalDate.of(2026, 11, 30), NOV_1);

	@Test
	void wholeFee_dueInThePeriod_isBilledAtItsOutstandingBalance() {
		List<FeeInvoiceLine> lines = InvoiceLineBuilder.linesFor(List.of(whole(10L, "800", NOV_1.plusDays(5))),
				NOVEMBER, Set.of());

		assertEquals(1, lines.size());
		assertEquals(0, new BigDecimal("800").compareTo(lines.getFirst().getAmount()));
	}

	@Test
	void wholeFee_dueInAnotherPeriod_orAlreadyPaid_isLeftOut() {
		assertTrue(InvoiceLineBuilder.linesFor(List.of(whole(10L, "800", NOV_1.plusMonths(1)),
				whole(11L, "0", NOV_1.plusDays(5))), NOVEMBER, Set.of()).isEmpty());
	}

	@Test
	void installments_onlyThoseDueInThePeriodAreBilled() {
		FeeBalance plan = new FeeBalance(20L, "Bus", new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1000"),
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, NOV_1,
				List.of(installment(1, NOV_1.plusDays(2), "500"), installment(2, NOV_1.plusMonths(1), "500")));

		List<FeeInvoiceLine> lines = InvoiceLineBuilder.linesFor(List.of(plan), NOVEMBER, Set.of());

		assertEquals(1, lines.size());
		assertEquals(1, lines.getFirst().getInstallmentNumber());
		assertEquals("Bus — installment 1", lines.getFirst().getDescription());
	}

	@Test
	void chargesAlreadyOnALiveInvoice_areNotBilledTwice() {
		List<FeeInvoiceLine> lines = InvoiceLineBuilder.linesFor(List.of(whole(10L, "800", NOV_1.plusDays(5))),
				NOVEMBER, Set.of(new ChargeKey(10L, null)));

		assertTrue(lines.isEmpty());
	}

	private static FeeBalance whole(Long structureId, String outstanding, LocalDate due) {
		BigDecimal amount = new BigDecimal(outstanding);
		return new FeeBalance(structureId, "Tuition", amount, BigDecimal.ZERO, amount, BigDecimal.ZERO, BigDecimal.ZERO,
				due);
	}

	private static InstallmentStatus installment(int sequence, LocalDate due, String amount) {
		BigDecimal value = new BigDecimal(amount);
		return new InstallmentStatus(sequence, due, value, BigDecimal.ZERO, value, InstallmentStatus.State.UPCOMING);
	}
}
