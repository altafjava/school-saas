package com.altafjava.school.domain.library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class CirculationTest {

	@Test
	void checkout_setsDueDate() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));

		assertEquals(LocalDate.of(2026, 4, 15), circulation.getDueDate());
	}

	@Test
	void returnBook_setsReturnedAtAndFine() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));

		circulation.returnBook(LocalDate.of(2026, 4, 20), BigDecimal.valueOf(25));

		assertEquals(LocalDate.of(2026, 4, 20), circulation.getReturnedAt());
		assertEquals(0, BigDecimal.valueOf(25).compareTo(circulation.getFineAmount()));
	}

	@Test
	void returnBook_alreadyReturned_throwsBusinessException() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));
		circulation.returnBook(LocalDate.of(2026, 4, 20), BigDecimal.ZERO);

		assertThrows(BusinessException.class,
				() -> circulation.returnBook(LocalDate.of(2026, 4, 21), BigDecimal.ZERO));
	}

	@Test
	void isOverdue_pastDueDateAndNotReturned_returnsTrue() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));

		assertTrue(circulation.isOverdue(LocalDate.of(2026, 4, 16)));
	}

	@Test
	void isOverdue_afterReturn_returnsFalse() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));
		circulation.returnBook(LocalDate.of(2026, 4, 20), BigDecimal.ZERO);

		assertFalse(circulation.isOverdue(LocalDate.of(2026, 4, 25)));
	}

	private static final LocalDate TODAY = LocalDate.of(2026, 4, 10);

	private Circulation loanDueOn(LocalDate dueDate) {
		return Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), dueDate);
	}

	@Test
	void returnBook_beforeItWasCheckedOut_throwsBusinessException() {
		Circulation circulation = loanDueOn(LocalDate.of(2026, 4, 15));

		assertThrows(BusinessException.class,
				() -> circulation.returnBook(LocalDate.of(2026, 3, 31), BigDecimal.ZERO));
	}

	@Test
	void renew_extendsTheDueDateAndCountsTheRenewal() {
		Circulation circulation = loanDueOn(LocalDate.of(2026, 4, 15));

		circulation.renew(TODAY, LocalDate.of(2026, 4, 29), 2);

		assertEquals(LocalDate.of(2026, 4, 29), circulation.getDueDate());
		assertEquals(1, circulation.getRenewalCount());
		assertEquals(TODAY, circulation.getLastRenewedAt());
	}

	@Test
	void renew_onTheDueDateItself_isStillAllowed() {
		Circulation circulation = loanDueOn(TODAY);

		circulation.renew(TODAY, TODAY.plusDays(14), 2);

		assertEquals(TODAY.plusDays(14), circulation.getDueDate());
	}

	@Test
	void renew_whenOverdue_throwsBusinessException() {
		Circulation circulation = loanDueOn(LocalDate.of(2026, 4, 9));

		assertThrows(BusinessException.class, () -> circulation.renew(TODAY, TODAY.plusDays(14), 2));
	}

	@Test
	void renew_afterTheMaximumRenewals_throwsBusinessException() {
		Circulation circulation = loanDueOn(LocalDate.of(2026, 4, 15));
		circulation.renew(TODAY, LocalDate.of(2026, 4, 29), 1);

		assertThrows(BusinessException.class, () -> circulation.renew(TODAY, LocalDate.of(2026, 5, 13), 1));
	}

	@Test
	void renew_withMaximumZero_neverAllowsRenewal() {
		Circulation circulation = loanDueOn(LocalDate.of(2026, 4, 15));

		assertThrows(BusinessException.class, () -> circulation.renew(TODAY, LocalDate.of(2026, 4, 29), 0));
	}

	@Test
	void renew_afterReturn_throwsBusinessException() {
		Circulation circulation = loanDueOn(LocalDate.of(2026, 4, 15));
		circulation.returnBook(LocalDate.of(2026, 4, 8), BigDecimal.ZERO);

		assertThrows(BusinessException.class, () -> circulation.renew(TODAY, LocalDate.of(2026, 4, 29), 2));
	}

	@Test
	void renew_toADateThatDoesNotExtendTheLoan_throwsBusinessException() {
		Circulation circulation = loanDueOn(LocalDate.of(2026, 4, 15));

		assertThrows(BusinessException.class, () -> circulation.renew(TODAY, LocalDate.of(2026, 4, 15), 2));
	}
}
