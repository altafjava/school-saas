package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.timetable.model.Venue;
import com.altafjava.school.domain.timetable.model.VenueType;
import com.altafjava.school.domain.timetable.repository.VenueRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VenueService {

	private final VenueRepository venueRepository;

	@Transactional(readOnly = true)
	public Page<Venue> list(Pageable pageable) {
		return venueRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Venue findByPublicId(String publicId) {
		return venueRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), TenantContext.getCurrentTenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + publicId));
	}

	@Transactional
	public Venue create(String code, String name, VenueType venueType, Integer capacity) {
		if (venueRepository.existsByCodeAndTenantId(code, TenantContext.getCurrentTenantId())) {
			throw new BusinessException("Venue code already exists: " + code);
		}
		return venueRepository.save(Venue.create(code, name, venueType, capacity));
	}

	@Transactional
	public Venue update(String publicId, String name, VenueType venueType, Integer capacity,
			ExpectedVersion expectedVersion) {
		Venue venue = findByPublicId(publicId);
		expectedVersion.verify(venue);
		venue.updateDetails(name, venueType, capacity);
		return venueRepository.save(venue);
	}

	@Transactional
	public Venue deactivate(String publicId) {
		Venue venue = findByPublicId(publicId);
		venue.deactivate();
		return venueRepository.save(venue);
	}

	@Transactional
	public Venue activate(String publicId) {
		Venue venue = findByPublicId(publicId);
		venue.activate();
		return venueRepository.save(venue);
	}
}
