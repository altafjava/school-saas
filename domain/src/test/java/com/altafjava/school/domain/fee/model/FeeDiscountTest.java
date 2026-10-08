package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class FeeDiscountTest {

	private FeeDiscount grant(DiscountType type, String value) {
		return FeeDiscount.grant(1L, 2L, type, new BigDecimal(value), "SIBLING", "second child", 9L);
	}

	@Test
	void percentage_isTakenOffTheGrossAmount() {
		assertEquals(0, new BigDecimal("125.00").compareTo(grant(DiscountType.PERCENTAGE, "12.5")
				.amountOn(new BigDecimal("1000.00"))));
	}

	@Test
	void fixed_isCappedAtTheGrossAmount() {
		assertEquals(0, new BigDecimal("80.00").compareTo(grant(DiscountType.FIXED, "500")
				.amountOn(new BigDecimal("80.00"))));
	}

	@Test
	void grant_rejectsNonPositiveAndOverHundredPercent() {
		assertThrows(BusinessException.class, () -> grant(DiscountType.FIXED, "0"));
		assertThrows(BusinessException.class, () -> grant(DiscountType.PERCENTAGE, "100.01"));
	}

	@Test
	void revoke_endsTheDiscount_andCannotBeRepeated() {
		FeeDiscount discount = grant(DiscountType.FIXED, "50");

		discount.revoke("no longer eligible");

		assertFalse(discount.isActive());
		assertThrows(BusinessException.class, () -> discount.revoke("again"));
	}
}
