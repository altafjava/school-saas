package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FeeStructureRevisionTest {

	@Test
	void record_capturesOldAndNewAmount() {
		FeeStructureRevision revision = FeeStructureRevision.record(1L, BigDecimal.valueOf(500),
				BigDecimal.valueOf(600));

		assertEquals(1L, revision.getFeeStructureId());
		assertEquals(BigDecimal.valueOf(500), revision.getOldAmount());
		assertEquals(BigDecimal.valueOf(600), revision.getNewAmount());
	}
}
