package com.altafjava.school.application.service;

import static com.altafjava.school.application.support.TestEntities.publicId;
import static com.altafjava.school.application.support.TestEntities.withId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class TermServiceTest {

	@Mock
	private TermRepository termRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;

	private TermService termService;

	@BeforeEach
	void setUp() {
		termService = new TermService(termRepository, academicYearRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNonExistentAcademicYear_throwsResourceNotFound() {
		when(academicYearRepository.findByPublicIdAndTenantId(publicId("academicYear", 99), 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> termService.create("Term 1", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 30), publicId("academicYear", 99).toString()));

		verify(termRepository, never()).save(any());
	}

	@Test
	void create_duplicateNameWithinSameAcademicYear_throwsIllegalArgument() {
		when(academicYearRepository.findByPublicIdAndTenantId(publicId("academicYear", 1), 1L)).thenReturn(Optional.of(withId(AcademicYear.class, 1L)));
		when(termRepository.existsByNameAndAcademicYearIdAndTenantId("Term 1", 1L, 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> termService.create("Term 1", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 30), publicId("academicYear", 1).toString()));
	}

	@Test
	void create_withValidAcademicYear_succeeds() {
		when(academicYearRepository.findByPublicIdAndTenantId(publicId("academicYear", 1), 1L)).thenReturn(Optional.of(withId(AcademicYear.class, 1L)));
		when(termRepository.existsByNameAndAcademicYearIdAndTenantId("Term 1", 1L, 1L)).thenReturn(false);
		when(termRepository.save(any(Term.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> termService.create("Term 1", LocalDate.of(2025, 6, 1),
				LocalDate.of(2025, 9, 30), publicId("academicYear", 1).toString()));
	}

	@Test
	void create_withRangeCoveringToday_marksNewTermCurrentAndUnmarksPrevious() {
		when(academicYearRepository.findByPublicIdAndTenantId(publicId("academicYear", 1), 1L)).thenReturn(Optional.of(withId(AcademicYear.class, 1L)));
		when(termRepository.existsByNameAndAcademicYearIdAndTenantId("Term 1", 1L, 1L)).thenReturn(false);
		when(termRepository.save(any(Term.class))).thenAnswer(inv -> inv.getArgument(0));
		Term previouslyCurrent = Term.create("Term 0", LocalDate.now().minusMonths(6), LocalDate.now().minusDays(1),
				1L);
		previouslyCurrent.markCurrent();
		when(termRepository.findCurrentByTenantId(1L)).thenReturn(java.util.Optional.of(previouslyCurrent));

		Term created = termService.create("Term 1", LocalDate.now().minusDays(1), LocalDate.now().plusMonths(3), publicId("academicYear", 1).toString());

		org.junit.jupiter.api.Assertions.assertTrue(created.isCurrent());
		org.junit.jupiter.api.Assertions.assertFalse(previouslyCurrent.isCurrent());
	}

	@Test
	void create_withFutureRange_doesNotTouchCurrentTerm() {
		when(academicYearRepository.findByPublicIdAndTenantId(publicId("academicYear", 1), 1L)).thenReturn(Optional.of(withId(AcademicYear.class, 1L)));
		when(termRepository.existsByNameAndAcademicYearIdAndTenantId("Term 1", 1L, 1L)).thenReturn(false);
		when(termRepository.save(any(Term.class))).thenAnswer(inv -> inv.getArgument(0));

		Term created = termService.create("Term 1", LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(4), publicId("academicYear", 1).toString());

		org.junit.jupiter.api.Assertions.assertFalse(created.isCurrent());
		verify(termRepository, never()).findCurrentByTenantId(any());
	}
}
