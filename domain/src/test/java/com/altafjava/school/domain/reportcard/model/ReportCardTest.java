package com.altafjava.school.domain.reportcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ReportCardTest {

	@Test
	void create_setsAllFields() {
		ReportCard reportCard = ReportCard.create(1L, 2L, 3L);

		assertEquals(1L, reportCard.getStudentId());
		assertEquals(2L, reportCard.getTermId());
		assertEquals(3L, reportCard.getDocumentIssuanceId());
		assertNotNull(reportCard.getGeneratedAt());
	}
}
