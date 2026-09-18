package com.altafjava.school.domain.health.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class HealthRecordTest {

	@Test
	void create_setsFields() {
		HealthRecord record = HealthRecord.create(1L, "O+", "Peanuts", "Asthma", "MMR, DTP");

		assertEquals(1L, record.getStudentId());
		assertEquals("O+", record.getBloodGroup());
		assertEquals("Peanuts", record.getAllergies());
		assertEquals("Asthma", record.getConditions());
		assertEquals("MMR, DTP", record.getImmunizations());
	}

	@Test
	void update_replacesAllMutableFields() {
		HealthRecord record = HealthRecord.create(1L, "O+", "Peanuts", "Asthma", "MMR, DTP");

		record.update("A-", "Pollen", "None", "MMR, DTP, HepB");

		assertEquals("A-", record.getBloodGroup());
		assertEquals("Pollen", record.getAllergies());
		assertEquals("None", record.getConditions());
		assertEquals("MMR, DTP, HepB", record.getImmunizations());
	}

	@Test
	void erasePii_clearsAllPhiFields() {
		HealthRecord record = HealthRecord.create(1L, "O+", "Peanuts", "Asthma", "MMR, DTP");

		record.erasePii();

		assertNull(record.getBloodGroup());
		assertNull(record.getAllergies());
		assertNull(record.getConditions());
		assertNull(record.getImmunizations());
		assertEquals(1L, record.getStudentId(), "studentId must survive erasure to keep the row addressable");
	}
}
