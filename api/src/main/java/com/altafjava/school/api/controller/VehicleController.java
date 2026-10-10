package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.altafjava.school.api.controller.api.VehicleApi;
import com.altafjava.school.api.dto.request.CreateVehicleRequest;
import com.altafjava.school.api.dto.request.UpdateVehicleRequest;
import com.altafjava.school.api.dto.response.VehicleResponse;
import com.altafjava.school.api.mapper.VehicleMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.VehicleService;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController implements VehicleApi {

	private final VehicleService vehicleService;
	private final VehicleMapper vehicleMapper;

	private final SpringDataPageableResolver pageableResolver;

	public VehicleController(VehicleService vehicleService, VehicleMapper vehicleMapper,
			SpringDataPageableResolver pageableResolver) {
		this.vehicleService = vehicleService;
		this.vehicleMapper = vehicleMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VEHICLE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<VehicleResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				vehicleService.list(pageableResolver.resolve(page, size)).map(vehicleMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VEHICLE_READ')")
	public ApiResponse<VehicleResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(vehicleMapper.toResponse(vehicleService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VEHICLE_WRITE')")
	public ApiResponse<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
		return ApiResponse.success(
				vehicleMapper.toResponse(vehicleService.create(request.registrationNumber(), request.capacity(),
						request.driverName(), request.driverContact())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VEHICLE_WRITE')")
	public ApiResponse<VehicleResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateVehicleRequest request) {
		return ApiResponse.success(vehicleMapper.toResponse(vehicleService.updateDetails(publicId, request.capacity(),
				request.driverName(), request.driverContact(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VEHICLE_WRITE')")
	public ApiResponse<VehicleResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(vehicleMapper.toResponse(vehicleService.deactivate(publicId)));
	}
}
