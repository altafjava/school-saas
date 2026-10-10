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
import com.altafjava.school.api.controller.api.AssetApi;
import com.altafjava.school.api.dto.request.CreateAssetRequest;
import com.altafjava.school.api.dto.request.UpdateAssetLocationRequest;
import com.altafjava.school.api.dto.response.AssetResponse;
import com.altafjava.school.api.mapper.AssetMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AssetService;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController implements AssetApi {

	private final AssetService assetService;
	private final AssetMapper assetMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AssetController(AssetService assetService, AssetMapper assetMapper,
			SpringDataPageableResolver pageableResolver) {
		this.assetService = assetService;
		this.assetMapper = assetMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<AssetResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(assetService.list(pageableResolver.resolve(page, size)).map(assetMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<AssetResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(assetMapper.toResponse(assetService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<AssetResponse> create(@Valid @RequestBody CreateAssetRequest request) {
		return ApiResponse.success(
				assetMapper.toResponse(assetService.create(request.assetCode(), request.name(), request.category(),
						request.purchaseDate(), request.purchaseCost(), request.location())));
	}

	@Override
	@PatchMapping("/{publicId}/location")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<AssetResponse> updateLocation(@PathVariable String publicId,
			@Valid @RequestBody UpdateAssetLocationRequest request) {
		return ApiResponse.success(assetMapper
				.toResponse(assetService.updateLocation(publicId, request.location(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/maintenance")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<AssetResponse> markUnderMaintenance(@PathVariable String publicId) {
		return ApiResponse.success(assetMapper.toResponse(assetService.markUnderMaintenance(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/available")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<AssetResponse> markAvailable(@PathVariable String publicId) {
		return ApiResponse.success(assetMapper.toResponse(assetService.markAvailable(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/dispose")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<AssetResponse> markDisposed(@PathVariable String publicId) {
		return ApiResponse.success(assetMapper.toResponse(assetService.markDisposed(publicId)));
	}
}
