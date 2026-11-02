package com.altafjava.school.application.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.school.application.service.HolidayService;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.holiday.service.SchoolWeek;
import lombok.RequiredArgsConstructor;

/** "Today" is the school's date, not the server's: the two differ for hours in most time zones. */
@Component
@RequiredArgsConstructor
public class HomeDayResolver {

	private final TenantFormattingService tenantFormattingService;
	private final AcademicYearRepository academicYearRepository;
	private final HolidayService holidayService;

	public HomeDay today(Long tenantId) {
		LocalDateTime now = LocalDateTime.now(tenantFormattingService.resolveZone(tenantId));
		LocalDate date = now.toLocalDate();
		Optional<AcademicYear> year = academicYearRepository
				.findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(tenantId);
		LocalDate yearStart = year.map(AcademicYear::getStartDate).filter(start -> !start.isAfter(date)).orElse(date);
		Set<LocalDate> holidays = holidayService.datesInRange(tenantId, yearStart, date);
		return new HomeDay(now, !SchoolWeek.isWeeklyOff(date) && !holidays.contains(date),
				year.map(AcademicYear::getId).orElse(null), yearStart, holidays);
	}
}
