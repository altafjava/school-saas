package com.altafjava.school.domain.library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class BookCopyTest {

	@Test
	void create_startsAvailable() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");

		assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
	}

	@Test
	void checkout_fromAvailable_succeeds() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");

		copy.checkout();

		assertEquals(BookCopyStatus.CHECKED_OUT, copy.getStatus());
	}

	@Test
	void checkout_whenAlreadyCheckedOut_throwsBusinessException() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");
		copy.checkout();

		assertThrows(BusinessException.class, copy::checkout);
	}

	@Test
	void returnCopy_afterCheckout_succeeds() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");
		copy.checkout();

		copy.returnCopy();

		assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
	}

	@Test
	void returnCopy_whenNotCheckedOut_throwsBusinessException() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");

		assertThrows(BusinessException.class, copy::returnCopy);
	}

	@Test
	void markLost_setsStatus() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");

		copy.markLost();

		assertEquals(BookCopyStatus.LOST, copy.getStatus());
	}

	@Test
	void hold_fromAvailable_setsTheCopyAside() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");

		copy.hold();

		assertEquals(BookCopyStatus.ON_HOLD, copy.getStatus());
	}

	@Test
	void hold_whenNotAvailable_throwsBusinessException() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");
		copy.checkout();

		assertThrows(BusinessException.class, copy::hold);
	}

	@Test
	void checkout_whileOnHold_throwsBusinessException() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");
		copy.hold();

		assertThrows(BusinessException.class, copy::checkout);
	}

	@Test
	void checkoutHeld_fromOnHold_succeeds() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");
		copy.hold();

		copy.checkoutHeld();

		assertEquals(BookCopyStatus.CHECKED_OUT, copy.getStatus());
	}

	@Test
	void checkoutHeld_whenNotOnHold_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> BookCopy.create(1L, "COPY-1").checkoutHeld());
	}

	@Test
	void releaseHold_putsTheCopyBackOnTheShelf() {
		BookCopy copy = BookCopy.create(1L, "COPY-1");
		copy.hold();

		copy.releaseHold();

		assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
	}

	@Test
	void releaseHold_whenNotOnHold_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> BookCopy.create(1L, "COPY-1").releaseHold());
	}
}
