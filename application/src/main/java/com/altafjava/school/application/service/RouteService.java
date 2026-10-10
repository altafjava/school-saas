package com.altafjava.school.application.service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.transport.model.Route;
import com.altafjava.school.domain.transport.model.RouteStop;
import com.altafjava.school.domain.transport.repository.RouteRepository;
import com.altafjava.school.domain.transport.repository.RouteStopRepository;

@Service
public class RouteService {

	private final RouteRepository routeRepository;
	private final RouteStopRepository routeStopRepository;

	public RouteService(RouteRepository routeRepository, RouteStopRepository routeStopRepository) {
		this.routeRepository = routeRepository;
		this.routeStopRepository = routeStopRepository;
	}

	@Transactional(readOnly = true)
	public Page<Route> list(String q, Pageable pageable) {
		return routeRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public Route findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return routeRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Route not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public List<RouteStop> listStops(String routePublicId) {
		Route route = findByPublicId(routePublicId);
		return routeStopRepository.findAllByRouteIdAndTenantIdOrderBySequenceOrderAsc(route.getId(),
				TenantContext.getCurrentTenantId());
	}

	@Transactional
	public Route create(String name, String code, String description) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (routeRepository.existsByCodeAndTenantId(code, tenantId)) {
			throw new BusinessException("Route code already exists: " + code);
		}
		return routeRepository.save(Route.create(name, code, description));
	}

	@Transactional
	public Route updateDetails(String publicId, String name, String code, String description,
			ExpectedVersion expectedVersion) {
		Route route = findByPublicId(publicId);
		expectedVersion.verify(route);
		route.updateDetails(name, code, description);
		return routeRepository.save(route);
	}

	@Transactional
	public Route deactivate(String publicId) {
		Route route = findByPublicId(publicId);
		route.deactivate();
		return routeRepository.save(route);
	}

	@Transactional
	public RouteStop addStop(String routePublicId, String stopName, int sequenceOrder, LocalTime pickupTime,
			LocalTime dropTime) {
		Route route = findByPublicId(routePublicId);
		return routeStopRepository.save(RouteStop.create(route.getId(), stopName, sequenceOrder, pickupTime,
				dropTime));
	}
}
