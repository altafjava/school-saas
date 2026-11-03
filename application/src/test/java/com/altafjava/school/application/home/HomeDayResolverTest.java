package com.altafjava.school.application.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.school.application.service.HolidayService;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;

@ExtendWith(MockitoExtension.class)
class HomeDayResolverTest {

	private static final Long TENANT_ID = 1L;

	@Mock
	private TenantFormattingService tenantFormattingService;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private HolidayService holidayService;
	private HomeDayResolver resolver;

	@BeforeEach
	void useUtc() {
		resolver = new HomeDayResolver(new SchoolClock(tenantFormattingService), academicYearRepository,
				holidayService);
		when(tenantFormattingService.resolveZone(TENANT_ID)).thenReturn(ZoneOffset.UTC);
	}

	@Test
	void today_usesTheTenantsZone_notTheServers() {
		ZoneId farEast = ZoneId.of("Pacific/Kiritimati");
		when(tenantFormattingService.resolveZone(TENANT_ID)).thenReturn(farEast);
		noYear();

		assertEquals(LocalDate.now(farEast), resolver.today(TENANT_ID).date());
	}

	@Test
	void today_withNoCurrentYear_hasNoYearAndCountsFromToday() {
		noYear();

		HomeDay day = resolver.today(TENANT_ID);

		assertFalse(day.hasAcademicYear());
		assertEquals(day.date(), day.yearStart());
	}

	@Test
	void today_withACurrentYear_countsFromItsStart() {
		LocalDate start = LocalDate.now(ZoneOffset.UTC).minusMonths(3);
		AcademicYear year = mock(AcademicYear.class);
		when(year.getId()).thenReturn(9L);
		when(year.getStartDate()).thenReturn(start);
		when(academicYearRepository.findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(TENANT_ID))
				.thenReturn(Optional.of(year));
		when(holidayService.datesInRange(eq(TENANT_ID), eq(start), any())).thenReturn(Set.of());

		HomeDay day = resolver.today(TENANT_ID);

		assertEquals(9L, day.academicYearId());
		assertEquals(start, day.yearStart());
	}

	@Test
	void today_onAHoliday_isNotASchoolDay() {
		noYear();
		when(holidayService.datesInRange(eq(TENANT_ID), any(), any()))
				.thenAnswer(call -> Set.of(call.getArgument(2, LocalDate.class)));

		assertFalse(resolver.today(TENANT_ID).schoolDay());
	}

	@Test
	void today_withoutAHoliday_isASchoolDayUnlessItIsSunday() {
		noYear();
		when(holidayService.datesInRange(eq(TENANT_ID), any(), any())).thenReturn(Set.of());

		HomeDay day = resolver.today(TENANT_ID);

		assertEquals(day.date().getDayOfWeek() != DayOfWeek.SUNDAY, day.schoolDay());
		assertTrue(day.holidays().isEmpty());
	}

	private void noYear() {
		when(academicYearRepository.findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(TENANT_ID))
				.thenReturn(Optional.empty());
	}
}
