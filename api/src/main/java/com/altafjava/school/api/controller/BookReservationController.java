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
import com.altafjava.school.api.controller.api.BookReservationApi;
import com.altafjava.school.api.dto.request.ReserveBookRequest;
import com.altafjava.school.api.dto.response.BookReservationResponse;
import com.altafjava.school.api.mapper.BookReservationMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.BookReservationService;

@RestController
@RequestMapping("/api/v1/book-reservations")
public class BookReservationController implements BookReservationApi {

	private final BookReservationService bookReservationService;
	private final BookReservationMapper bookReservationMapper;
	private final SpringDataPageableResolver pageableResolver;

	public BookReservationController(BookReservationService bookReservationService,
			BookReservationMapper bookReservationMapper, SpringDataPageableResolver pageableResolver) {
		this.bookReservationService = bookReservationService;
		this.bookReservationMapper = bookReservationMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CIRCULATION_MANAGE')")
	@SortableBy({ "reservedAt", "status" })
	public ApiResponse<com.altafjava.platform.core.model.Page<BookReservationResponse>> list(
			@RequestParam(required = false) String bookPublicId,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				bookReservationService.list(bookPublicId, studentPublicId, pageableResolver.resolve(page, size))
						.map(bookReservationMapper::toResponse)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CIRCULATION_MANAGE')")
	public ApiResponse<BookReservationResponse> reserve(@Valid @RequestBody ReserveBookRequest request) {
		return ApiResponse.success(bookReservationMapper
				.toResponse(bookReservationService.reserve(request.bookPublicId(), request.studentPublicId())));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CIRCULATION_MANAGE')")
	public ApiResponse<BookReservationResponse> cancel(@PathVariable String publicId) {
		return ApiResponse.success(bookReservationMapper.toResponse(bookReservationService.cancel(publicId)));
	}
}
