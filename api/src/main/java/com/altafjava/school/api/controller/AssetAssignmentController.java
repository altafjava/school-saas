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
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.school.api.controller.api.AssetAssignmentApi;
import com.altafjava.school.api.dto.request.AssignAssetRequest;
import com.altafjava.school.api.dto.request.ReturnAssetRequest;
import com.altafjava.school.api.dto.response.AssetAssignmentResponse;
import com.altafjava.school.api.mapper.AssetAssignmentMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AssetAssignmentService;

@RestController
@RequestMapping("/api/v1/assets/{assetPublicId}/assignments")
public class AssetAssignmentController implements AssetAssignmentApi {

	private final AssetAssignmentService assetAssignmentService;
	private final AssetAssignmentMapper assetAssignmentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AssetAssignmentController(AssetAssignmentService assetAssignmentService,
			AssetAssignmentMapper assetAssignmentMapper, SpringDataPageableResolver pageableResolver) {
		this.assetAssignmentService = assetAssignmentService;
		this.assetAssignmentMapper = assetAssignmentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<AssetAssignmentResponse>> list(
			@PathVariable String assetPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(assetAssignmentService.listForAsset(assetPublicId, pageableResolver.resolve(page, size))
						.map(assetAssignmentMapper::toResponse)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<AssetAssignmentResponse> assign(@PathVariable String assetPublicId,
			@Valid @RequestBody AssignAssetRequest request) {
		return ApiResponse.success(assetAssignmentMapper.toResponse(assetAssignmentService.assign(assetPublicId,
				request.assignedToType(), request.assignedToPublicId(), request.assignedAt())));
	}

	@Override
	@PatchMapping("/{assignmentPublicId}/return")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public ApiResponse<AssetAssignmentResponse> markReturned(@PathVariable String assetPublicId,
			@PathVariable String assignmentPublicId, @Valid @RequestBody ReturnAssetRequest request) {
		return ApiResponse.success(assetAssignmentMapper.toResponse(
				assetAssignmentService.markReturned(assignmentPublicId, request.returnedAt())));
	}
}
