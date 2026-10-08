package com.altafjava.school.domain.fee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.domain.fee.model.FeeInstallment;
import com.altafjava.school.domain.fee.service.InstallmentPlanFactory.Slot;

class InstallmentPlanFactoryTest {

	private static final LocalDate START = LocalDate.of(2026, 7, 1);

	@Test
	void evenSplit_alwaysTotalsExactlyOneHundredPercent_withTheRemainderOnTheLast() {
		List<FeeInstallment> plan = InstallmentPlanFactory.evenSplit(1L, 2L, 3, START, 1);

		assertEquals(10_000, plan.stream().mapToInt(FeeInstallment::getShareBasisPoints).sum());
		assertEquals(3333, plan.get(0).getShareBasisPoints());
		assertEquals(3334, plan.get(2).getShareBasisPoints());
		assertEquals(START.plusMonths(2), plan.get(2).getDueDate());
		assertEquals(List.of(1, 2, 3), plan.stream().map(FeeInstallment::getSequenceNumber).toList());
	}

	@Test
	void evenSplit_rejectsCountsOutsideTheSupportedRange() {
		assertThrows(BusinessException.class, () -> InstallmentPlanFactory.evenSplit(1L, 2L, 1, START, 1));
		assertThrows(BusinessException.class, () -> InstallmentPlanFactory.evenSplit(1L, 2L, 25, START, 1));
		assertThrows(BusinessException.class, () -> InstallmentPlanFactory.evenSplit(1L, 2L, 3, START, 0));
	}

	@Test
	void fromSlots_acceptsAnUnevenScheduleThatTotals100() {
		List<FeeInstallment> plan = InstallmentPlanFactory.fromSlots(1L, 2L,
				List.of(new Slot(START, 5000), new Slot(START.plusMonths(3), 2500),
						new Slot(START.plusMonths(6), 2500)));

		assertEquals(3, plan.size());
	}

	@Test
	void fromSlots_rejectsSharesThatDoNotTotal100() {
		assertThrows(BusinessException.class, () -> InstallmentPlanFactory.fromSlots(1L, 2L,
				List.of(new Slot(START, 5000), new Slot(START.plusMonths(1), 4000))));
	}

	@Test
	void fromSlots_rejectsDueDatesThatDoNotIncrease() {
		assertThrows(BusinessException.class, () -> InstallmentPlanFactory.fromSlots(1L, 2L,
				List.of(new Slot(START, 5000), new Slot(START, 5000))));
	}
}
