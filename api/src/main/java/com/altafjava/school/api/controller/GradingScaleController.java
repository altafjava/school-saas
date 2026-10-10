package com.altafjava.school.api.controller;

import java.util.List;
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
import com.altafjava.school.api.controller.api.GradingScaleApi;
import com.altafjava.school.api.dto.request.CreateGradingScaleRequest;
import com.altafjava.school.api.dto.request.UpdateGradingScaleThresholdsRequest;
import com.altafjava.school.api.dto.response.GradingScaleResponse;
import com.altafjava.school.api.mapper.GradingScaleMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.application.service.GradingScaleThresholdInput;
import com.altafjava.school.domain.curriculum.model.GradingScale;

@RestController
@RequestMapping("/api/v1/grading-scales")
public class GradingScaleController implements GradingScaleApi {

	private final GradingScaleService gradingScaleService;
	private final GradingScaleMapper gradingScaleMapper;

	private final SpringDataPageableResolver pageableResolver;

	public GradingScaleController(GradingScaleService gradingScaleService, GradingScaleMapper gradingScaleMapper,
			SpringDataPageableResolver pageableResolver) {
		this.gradingScaleService = gradingScaleService;
		this.gradingScaleMapper = gradingScaleMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GRADING_SCALE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<GradingScaleResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(gradingScaleService.list(pageableResolver.resolve(page, size)).map(this::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GRADING_SCALE_READ')")
	public ApiResponse<GradingScaleResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(toResponse(gradingScaleService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GRADING_SCALE_WRITE')")
	public ApiResponse<GradingScaleResponse> create(@Valid @RequestBody CreateGradingScaleRequest request) {
		GradingScale scale = gradingScaleService.create(request.name(), toInputs(request), request.isDefault());
		return ApiResponse.success(toResponse(scale));
	}

	@Override
	@PatchMapping("/{publicId}/thresholds")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GRADING_SCALE_WRITE')")
	public ApiResponse<GradingScaleResponse> updateThresholds(@PathVariable String publicId,
			@Valid @RequestBody UpdateGradingScaleThresholdsRequest request) {
		var thresholds = request.thresholds().stream()
				.map(t -> new GradingScaleThresholdInput(t.letter(), t.minPercentage(), t.points()))
				.toList();
		return ApiResponse.success(
				toResponse(gradingScaleService.updateThresholds(publicId, thresholds, request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/default")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GRADING_SCALE_WRITE')")
	public ApiResponse<GradingScaleResponse> markAsDefault(@PathVariable String publicId) {
		return ApiResponse.success(toResponse(gradingScaleService.markAsDefault(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GRADING_SCALE_WRITE')")
	public ApiResponse<GradingScaleResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(toResponse(gradingScaleService.deactivate(publicId)));
	}

	private List<GradingScaleThresholdInput> toInputs(CreateGradingScaleRequest request) {
		return request.thresholds().stream()
				.map(t -> new GradingScaleThresholdInput(t.letter(), t.minPercentage(), t.points()))
				.toList();
	}

	private GradingScaleResponse toResponse(GradingScale scale) {
		var thresholds = gradingScaleService.listThresholds(scale.getPublicId().toString()).stream()
				.map(gradingScaleMapper::toThreshold)
				.toList();
		return new GradingScaleResponse(scale.getPublicId().toString(), scale.getVersion(), scale.getName(),
				scale.isDefault(),
				scale.isActive(), thresholds);
	}
}
