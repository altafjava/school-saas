package com.altafjava.school.application.alert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import com.altafjava.school.domain.fee.model.FeeBalance;

/**
 * One unpaid amount and the date it falls due: an installment's remainder, or a whole balance
 * when the fee has no installment plan. A balance with no due date cannot be reminded about.
 */
record FeeDueItem(LocalDate dueDate, BigDecimal outstanding) {

	static List<FeeDueItem> unpaid(Collection<FeeBalance> balances) {
		return balances.stream().flatMap(balance -> balance.installments().isEmpty()
				? Stream.of(new FeeDueItem(balance.dueDate(), balance.outstandingBalance()))
				: balance.installments().stream()
						.map(installment -> new FeeDueItem(installment.dueDate(), installment.outstandingAmount())))
				.filter(item -> item.dueDate() != null && item.outstanding() != null && item.outstanding().signum() > 0)
				.toList();
	}

	static BigDecimal total(List<FeeDueItem> items) {
		return items.stream().map(FeeDueItem::outstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	static LocalDate earliest(List<FeeDueItem> items) {
		return items.stream().map(FeeDueItem::dueDate).min(LocalDate::compareTo).orElseThrow();
	}

	static Predicate<FeeDueItem> dueOn(LocalDate date) {
		return item -> item.dueDate().isEqual(date);
	}
}
