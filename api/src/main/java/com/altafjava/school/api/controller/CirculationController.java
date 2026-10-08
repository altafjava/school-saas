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
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.school.api.controller.api.CirculationApi;
import com.altafjava.school.api.dto.request.CheckoutBookRequest;
import com.altafjava.school.api.dto.request.ReturnBookRequest;
import com.altafjava.school.api.dto.response.CirculationResponse;
import com.altafjava.school.api.mapper.CirculationMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CirculationService;

@RestController
@RequestMapping("/api/v1/circulations")
public class CirculationController implements CirculationApi {

	private final CirculationService circulationService;
	private final CirculationMapper circulationMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CirculationController(CirculationService circulationService, CirculationMapper circulationMapper,
			SpringDataPageableResolver pageableResolver) {
		this.circulationService = circulationService;
		this.circulationMapper = circulationMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CIRCULATION_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<CirculationResponse>> listForStudent(
			@RequestParam String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(circulationService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
						.map(circulationMapper::toResponse)));
	}

	@Override
	@PostMapping("/checkout")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CIRCULATION_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<CirculationResponse> checkout(@Valid @RequestBody CheckoutBookRequest request) {
		return ApiResponse.success(circulationMapper.toResponse(
				circulationService.checkout(request.bookCopyPublicId(), request.studentPublicId())));
	}

	@Override
	@PatchMapping("/{publicId}/return")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CIRCULATION_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<CirculationResponse> returnBook(@PathVariable String publicId,
			@Valid @RequestBody ReturnBookRequest request) {
		return ApiResponse
				.success(circulationMapper.toResponse(circulationService.returnBook(publicId, request.returnedAt())));
	}

	@Override
	@PatchMapping("/{publicId}/renew")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CIRCULATION_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<CirculationResponse> renew(@PathVariable String publicId) {
		return ApiResponse.success(circulationMapper.toResponse(circulationService.renew(publicId)));
	}
}
