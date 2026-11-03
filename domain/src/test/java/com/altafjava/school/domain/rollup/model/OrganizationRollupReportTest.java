package com.altafjava.school.domain.rollup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizationRollupReportTest {

	@Test
	void of_computesTotalsAcrossAllCampuses() {
		CampusRollup campusA = new CampusRollup(UUID.randomUUID(), "Campus A", 100,
				new AttendanceRollup(90, 0, 5, 3, 2),
				FeeRollup.of(BigDecimal.valueOf(10000), BigDecimal.valueOf(6000)));
		CampusRollup campusB = new CampusRollup(UUID.randomUUID(), "Campus B", 50,
				new AttendanceRollup(40, 0, 5, 5, 0), FeeRollup.of(BigDecimal.valueOf(5000), BigDecimal.valueOf(5500)));

		OrganizationRollupReport report = OrganizationRollupReport.of(
				UUID.randomUUID(), "Acme School Group", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
				List.of(campusA, campusB));

		assertEquals(2, report.campuses().size());
		assertEquals(150, report.totals().activeStudentCount());
		assertEquals(new AttendanceRollup(130, 0, 10, 8, 2), report.totals().attendance());
		assertEquals(BigDecimal.valueOf(15000), report.totals().fees().totalDue());
		assertEquals(BigDecimal.valueOf(11500), report.totals().fees().totalPaid());
		// Per-campus outstanding/overpaid are clipped at zero before summing (campusA: due 10000,
		// paid 6000 -> outstanding 4000; campusB: due 5000, paid 5500 -> overpaid 500) — a
		// campus's overpayment must never silently net against another campus's shortfall.
		assertEquals(BigDecimal.valueOf(4000), report.totals().fees().outstandingBalance());
		assertEquals(BigDecimal.valueOf(500), report.totals().fees().overpaidAmount());
	}

	@Test
	void of_noCampuses_totalsAreZero() {
		OrganizationRollupReport report = OrganizationRollupReport.of(
				UUID.randomUUID(), "Empty Group", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), List.of());

		assertEquals(0, report.totals().activeStudentCount());
		assertEquals(AttendanceRollup.ZERO, report.totals().attendance());
		assertEquals(FeeRollup.ZERO, report.totals().fees());
	}
}
