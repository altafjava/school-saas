package com.altafjava.school.domain.lifecycle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.student.model.EnrollmentStatus;

class LifecycleTransitionTest {

	@Test
	void record_keepsTheEffectiveDateAndReason() {
		LocalDate effectiveOn = LocalDate.of(2026, 4, 1);
		LifecycleTransition transition = LifecycleTransition.record(null, 5L, LifecycleStage.ENROLLED,
				LifecycleStage.WITHDRAWN, "Relocated", effectiveOn, 9L);

		assertEquals(effectiveOn, transition.getEffectiveOn());
		assertEquals("Relocated", transition.getReason());
	}

	@Test
	void record_needsAnEffectiveDate() {
		assertThrows(BusinessException.class, () -> LifecycleTransition.record(null, 5L, LifecycleStage.ENROLLED,
				LifecycleStage.WITHDRAWN, null, null, null));
	}

	@Test
	void record_needsAnAdmissionOrAStudent() {
		assertThrows(BusinessException.class, () -> LifecycleTransition.record(null, null, null,
				LifecycleStage.SUBMITTED, null, null, null));
	}

	@Test
	void record_rejectsANoOpTransition() {
		assertThrows(BusinessException.class, () -> LifecycleTransition.record(1L, null, LifecycleStage.APPROVED,
				LifecycleStage.APPROVED, null, null, null));
	}

	@Test
	void everyAdmissionAndEnrollmentStatusMapsToAStage() {
		for (AdmissionStatus status : AdmissionStatus.values()) {
			assertEquals(status.name(), LifecycleStage.of(status).name());
		}
		assertEquals(LifecycleStage.ENROLLED, LifecycleStage.of(EnrollmentStatus.ACTIVE));
		for (EnrollmentStatus status : EnrollmentStatus.values()) {
			LifecycleStage.of(status);
		}
	}
}
