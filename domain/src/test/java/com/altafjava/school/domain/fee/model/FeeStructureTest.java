package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FeeStructureTest {

	@Test
	void create_setsFields() {
		FeeStructure structure = FeeStructure.create("Tuition", BigDecimal.valueOf(500), FeeFrequency.MONTHLY,
				"STANDARD");

		assertEquals("Tuition", structure.getName());
		assertEquals(BigDecimal.valueOf(500), structure.getAmount());
		assertEquals(FeeFrequency.MONTHLY, structure.getFrequency());
		assertEquals("STANDARD", structure.getPlanType());
	}

	@Test
	void reviseAmount_updatesAmount() {
		FeeStructure structure = FeeStructure.create("Tuition", BigDecimal.valueOf(500), FeeFrequency.MONTHLY,
				"STANDARD");

		structure.reviseAmount(BigDecimal.valueOf(600));

		assertEquals(BigDecimal.valueOf(600), structure.getAmount());
	}

	@Test
	void configureLateFeePolicy_setsGraceDaysAndPercentage() {
		FeeStructure structure = FeeStructure.create("Tuition", BigDecimal.valueOf(500), FeeFrequency.MONTHLY,
				"STANDARD");

		structure.configureLateFeePolicy(7, BigDecimal.valueOf(1.5));

		assertEquals(7, structure.getGraceDays());
		assertEquals(BigDecimal.valueOf(1.5), structure.getLateFeePercentage());
	}
}
