package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.school.api.controller.api.RouteApi;
import com.altafjava.school.api.dto.request.AddRouteStopRequest;
import com.altafjava.school.api.dto.request.CreateRouteRequest;
import com.altafjava.school.api.dto.request.UpdateRouteRequest;
import com.altafjava.school.api.dto.response.RouteResponse;
import com.altafjava.school.api.dto.response.RouteStopResponse;
import com.altafjava.school.api.mapper.RouteMapper;
import com.altafjava.school.api.mapper.RouteStopMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.RouteService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController implements RouteApi {

	private final RouteService routeService;
	private final RouteMapper routeMapper;
	private final RouteStopMapper routeStopMapper;

	private final SpringDataPageableResolver pageableResolver;

	private final ReferenceSummaries referenceSummaries;

	public RouteController(RouteService routeService, RouteMapper routeMapper, RouteStopMapper routeStopMapper,
			SpringDataPageableResolver pageableResolver,
			ReferenceSummaries referenceSummaries) {
		this.referenceSummaries = referenceSummaries;
		this.routeService = routeService;
		this.routeMapper = routeMapper;
		this.routeStopMapper = routeStopMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.TRANSPORT_ROUTE_READ)
	public ApiResponse<com.altafjava.platform.core.model.Page<RouteResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(
						routeService.list(q, pageableResolver.resolve(page, size)).map(routeMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.TRANSPORT_ROUTE_READ)
	public ApiResponse<RouteResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(routeMapper.toResponse(routeService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.TRANSPORT_ROUTE_WRITE)
	public ApiResponse<RouteResponse> create(@Valid @RequestBody CreateRouteRequest request) {
		return ApiResponse.success(
				routeMapper.toResponse(routeService.create(request.name(), request.code(), request.description())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.TRANSPORT_ROUTE_WRITE)
	public ApiResponse<RouteResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateRouteRequest request) {
		return ApiResponse.success(routeMapper.toResponse(
				routeService.updateDetails(publicId, request.name(), request.code(), request.description(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@RequiresPermission(SchoolPermissions.TRANSPORT_ROUTE_WRITE)
	public ApiResponse<RouteResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(routeMapper.toResponse(routeService.deactivate(publicId)));
	}

	@Override
	@GetMapping("/{publicId}/stops")
	@RequiresPermission(SchoolPermissions.TRANSPORT_ROUTE_READ)
	public ApiResponse<List<RouteStopResponse>> listStops(@PathVariable String publicId) {
		return ApiResponse
				.success(referenceSummaries.mapList(routeService.listStops(publicId), routeStopMapper::toResponse));
	}

	@Override
	@PostMapping("/{publicId}/stops")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.TRANSPORT_ROUTE_WRITE)
	public ApiResponse<RouteStopResponse> addStop(@PathVariable String publicId,
			@Valid @RequestBody AddRouteStopRequest request) {
		return ApiResponse.success(
				routeStopMapper.toResponse(routeService.addStop(publicId, request.stopName(), request.sequenceOrder(),
						request.pickupTime(), request.dropTime())));
	}
}
