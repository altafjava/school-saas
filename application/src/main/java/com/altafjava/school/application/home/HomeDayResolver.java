package com.altafjava.school.application.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.service.HolidayService;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.holiday.service.SchoolWeek;
import lombok.RequiredArgsConstructor;

/** The school's day: its date, whether it is a school day, and the academic year it falls in. */
@Component
@RequiredArgsConstructor
public class HomeDayResolver {

	private final SchoolClock schoolClock;
	private final AcademicYearRepository academicYearRepository;
	private final HolidayService holidayService;

	public HomeDay today(Long tenantId) {
		LocalDateTime now = schoolClock.now(tenantId);
		LocalDate date = now.toLocalDate();
		Optional<AcademicYear> year = academicYearRepository
				.findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(tenantId);
		LocalDate yearStart = year.map(AcademicYear::getStartDate).filter(start -> !start.isAfter(date)).orElse(date);
		Set<LocalDate> holidays = holidayService.datesInRange(tenantId, yearStart, date);
		return new HomeDay(now, !SchoolWeek.isWeeklyOff(date) && !holidays.contains(date),
				year.map(AcademicYear::getId).orElse(null), yearStart, holidays);
	}
}
