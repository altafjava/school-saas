package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.holiday.model.Holiday;
import com.altafjava.school.domain.holiday.repository.HolidayRepository;
import com.altafjava.school.domain.holiday.service.HolidayDateRangeResolver;

@Service
public class HolidayService {

	private final HolidayRepository holidayRepository;
	private final HolidayDateRangeResolver holidayDateRangeResolver = new HolidayDateRangeResolver();

	public HolidayService(HolidayRepository holidayRepository) {
		this.holidayRepository = holidayRepository;
	}

	@Transactional(readOnly = true)
	public Page<Holiday> list(Pageable pageable) {
		return holidayRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Holiday findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return holidayRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + publicId));
	}

	@Transactional
	public Holiday create(LocalDate date, String name, boolean recurring) {
		return holidayRepository.save(Holiday.create(date, name, recurring));
	}

	@Transactional
	public Holiday updateDetails(String publicId, LocalDate date, String name, boolean recurring,
			ExpectedVersion expectedVersion) {
		Holiday holiday = findByPublicId(publicId);
		expectedVersion.verify(holiday);
		holiday.updateDetails(date, name, recurring);
		return holidayRepository.save(holiday);
	}

	@Transactional
	public void delete(String publicId) {
		Holiday holiday = findByPublicId(publicId);
		holiday.softDelete("holiday-deletion");
		holidayRepository.save(holiday);
	}

	/**
	 * Every holiday date that falls within [from, to] (inclusive) — a recurring holiday contributes
	 * one date per year touched by the range, matched by month/day regardless of the year stored on
	 * its own {@code date}. Used to exclude holidays from attendance-percentage denominators and
	 * leave-day counts.
	 */
	@Transactional(readOnly = true)
	public Set<LocalDate> datesInRange(Long tenantId, LocalDate from, LocalDate to) {
		return holidayDateRangeResolver.resolve(holidayRepository.findAllByTenantId(tenantId), from, to);
	}
}
