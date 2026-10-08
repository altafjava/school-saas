package com.altafjava.school.domain.fee.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.domain.fee.model.FeeInstallment;

/** Builds valid installment plans; every plan it returns totals exactly 100 % with rising due dates. */
public final class InstallmentPlanFactory {

	public static final int MIN_INSTALLMENTS = 2;
	public static final int MAX_INSTALLMENTS = 24;

	public record Slot(LocalDate dueDate, int shareBasisPoints) {
	}

	private InstallmentPlanFactory() {
	}

	/** {@code count} equal shares, {@code intervalMonths} apart; the last share absorbs any remainder. */
	public static List<FeeInstallment> evenSplit(Long studentId, Long feeStructureId, int count,
			LocalDate firstDueDate, int intervalMonths) {
		requireCount(count);
		if (intervalMonths < 1) {
			throw new BusinessException("Installments must be at least one month apart");
		}
		int equalShare = FeeInstallment.TOTAL_BASIS_POINTS / count;
		List<Slot> slots = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int share = i == count - 1 ? FeeInstallment.TOTAL_BASIS_POINTS - equalShare * (count - 1) : equalShare;
			slots.add(new Slot(firstDueDate.plusMonths((long) i * intervalMonths), share));
		}
		return fromSlots(studentId, feeStructureId, slots);
	}

	public static List<FeeInstallment> fromSlots(Long studentId, Long feeStructureId, List<Slot> slots) {
		requireCount(slots.size());
		int total = slots.stream().mapToInt(Slot::shareBasisPoints).sum();
		if (total != FeeInstallment.TOTAL_BASIS_POINTS) {
			throw new BusinessException("Installment shares must total 100%, got " + total / 100.0 + "%");
		}
		List<FeeInstallment> plan = new ArrayList<>(slots.size());
		LocalDate previous = null;
		for (int i = 0; i < slots.size(); i++) {
			Slot slot = slots.get(i);
			if (previous != null && !slot.dueDate().isAfter(previous)) {
				throw new BusinessException("Installment due dates must be strictly increasing");
			}
			previous = slot.dueDate();
			plan.add(FeeInstallment.of(studentId, feeStructureId, i + 1, slot.dueDate(), slot.shareBasisPoints()));
		}
		return plan;
	}

	private static void requireCount(int count) {
		if (count < MIN_INSTALLMENTS || count > MAX_INSTALLMENTS) {
			throw new BusinessException("An installment plan needs " + MIN_INSTALLMENTS + " to " + MAX_INSTALLMENTS
					+ " installments, got " + count);
		}
	}
}
