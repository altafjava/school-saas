package com.altafjava.school.api.controller;

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
import com.altafjava.platform.core.feature.annotation.FeatureEnabled;
import com.altafjava.school.api.controller.api.RoomApi;
import com.altafjava.school.api.dto.request.CreateRoomRequest;
import com.altafjava.school.api.dto.request.UpdateRoomRequest;
import com.altafjava.school.api.dto.response.RoomResponse;
import com.altafjava.school.api.mapper.RoomMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.security.SchoolFeatures;
import com.altafjava.school.application.service.RoomService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@RestController
@FeatureEnabled(SchoolFeatures.CAMPUS_OPS)
@RequestMapping("/api/v1/rooms")
public class RoomController implements RoomApi {

	private final RoomService roomService;
	private final RoomMapper roomMapper;

	private final SpringDataPageableResolver pageableResolver;

	private final ReferenceSummaries referenceSummaries;

	public RoomController(RoomService roomService, RoomMapper roomMapper,
			SpringDataPageableResolver pageableResolver,
			ReferenceSummaries referenceSummaries) {
		this.referenceSummaries = referenceSummaries;
		this.roomService = roomService;
		this.roomMapper = roomMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.ROOM_READ)
	public ApiResponse<com.altafjava.platform.core.model.Page<RoomResponse>> listForBuilding(
			@RequestParam String hostelBuildingPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				roomService.listForBuilding(hostelBuildingPublicId, q, pageableResolver.resolve(page, size)),
				roomMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.ROOM_READ)
	public ApiResponse<RoomResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(roomMapper.toResponse(roomService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.ROOM_WRITE)
	public ApiResponse<RoomResponse> create(
			@RequestParam String hostelBuildingPublicId,
			@Valid @RequestBody CreateRoomRequest request) {
		return ApiResponse.success(roomMapper.toResponse(
				roomService.create(hostelBuildingPublicId, request.roomNumber(), request.capacity())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.ROOM_WRITE)
	public ApiResponse<RoomResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateRoomRequest request) {
		return ApiResponse.success(
				roomMapper.toResponse(roomService.updateDetails(publicId, request.roomNumber(), request.capacity(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@RequiresPermission(SchoolPermissions.ROOM_WRITE)
	public ApiResponse<RoomResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(roomMapper.toResponse(roomService.deactivate(publicId)));
	}
}
