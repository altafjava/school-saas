package com.altafjava.school.application.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.filter.EventFilter;
import com.altafjava.school.domain.event.model.Event;
import com.altafjava.school.domain.event.repository.EventRepository;

@Service
public class EventService {

	private final EventRepository eventRepository;

	public EventService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@Transactional(readOnly = true)
	public Page<Event> list(EventFilter filter, Pageable pageable) {
		return eventRepository.search(TenantContext.getCurrentTenantId(),
				filter.dates().from() != null ? filter.dates().from().atStartOfDay() : null,
				filter.dates().to() != null ? filter.dates().to().plusDays(1).atStartOfDay() : null,
				LikePattern.contains(filter.q()), pageable);
	}

	@Transactional(readOnly = true)
	public Event findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return eventRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found: " + publicId));
	}

	@Transactional
	public Event create(String title, String description, LocalDateTime eventDate, String location,
			boolean registrationRequired, Integer capacity) {
		return eventRepository.save(
				Event.create(title, description, eventDate, location, registrationRequired, capacity));
	}

	@Transactional
	public Event updateDetails(String publicId, String title, String description, LocalDateTime eventDate,
			String location, ExpectedVersion expectedVersion) {
		Event event = findByPublicId(publicId);
		expectedVersion.verify(event);
		event.updateDetails(title, description, eventDate, location);
		return eventRepository.save(event);
	}

	@Transactional
	public Event cancel(String publicId) {
		Event event = findByPublicId(publicId);
		event.cancel();
		return eventRepository.save(event);
	}
}
