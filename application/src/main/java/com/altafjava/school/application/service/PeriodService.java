package com.altafjava.school.application.service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;

@Service
public class PeriodService {

	private final PeriodRepository periodRepository;

	public PeriodService(PeriodRepository periodRepository) {
		this.periodRepository = periodRepository;
	}

	@Transactional(readOnly = true)
	public Page<Period> listPeriods(String q, Pageable pageable) {
		return periodRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public List<Period> listAllOrdered() {
		return periodRepository.findAllByTenantIdOrderByDisplayOrderAsc(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public Period findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return periodRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Period not found: " + publicId));
	}

	@Transactional
	public Period create(String name, LocalTime startTime, LocalTime endTime, int displayOrder) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!startTime.isBefore(endTime)) {
			throw new BusinessException("Period startTime must be before endTime");
		}
		if (periodRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new BusinessException("Period already exists: " + name);
		}
		Period period = Period.create(name, startTime, endTime, displayOrder);
		return periodRepository.save(period);
	}
}
