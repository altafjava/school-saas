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
import com.altafjava.school.api.controller.api.BookApi;
import com.altafjava.school.api.dto.request.AddBookCopyRequest;
import com.altafjava.school.api.dto.request.CreateBookRequest;
import com.altafjava.school.api.dto.response.BookCopyResponse;
import com.altafjava.school.api.dto.response.BookResponse;
import com.altafjava.school.api.mapper.BookCopyMapper;
import com.altafjava.school.api.mapper.BookMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.BookCatalogService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@RestController
@RequestMapping("/api/v1/books")
public class BookController implements BookApi {

	private final BookCatalogService bookCatalogService;
	private final BookMapper bookMapper;
	private final BookCopyMapper bookCopyMapper;

	private final SpringDataPageableResolver pageableResolver;

	private final ReferenceSummaries referenceSummaries;

	public BookController(BookCatalogService bookCatalogService, BookMapper bookMapper,
			BookCopyMapper bookCopyMapper, SpringDataPageableResolver pageableResolver,
			ReferenceSummaries referenceSummaries) {
		this.referenceSummaries = referenceSummaries;
		this.bookCatalogService = bookCatalogService;
		this.bookMapper = bookMapper;
		this.bookCopyMapper = bookCopyMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.BOOK_READ)
	@SortableBy({ "title", "author", "category" })
	public ApiResponse<com.altafjava.platform.core.model.Page<BookResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				bookCatalogService.searchBooks(pageableResolver.resolve(page, size), q).map(bookMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.BOOK_READ)
	public ApiResponse<BookResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(bookMapper.toResponse(bookCatalogService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.BOOK_MANAGE)
	public ApiResponse<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
		return ApiResponse.success(
				bookMapper.toResponse(bookCatalogService.createBook(request.isbn(), request.title(), request.author(),
						request.publisher(), request.category())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@RequiresPermission(SchoolPermissions.BOOK_MANAGE)
	public ApiResponse<BookResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(bookMapper.toResponse(bookCatalogService.deactivateBook(publicId)));
	}

	@Override
	@GetMapping("/{publicId}/copies")
	@RequiresPermission(SchoolPermissions.BOOK_READ)
	public ApiResponse<com.altafjava.platform.core.model.Page<BookCopyResponse>> listCopies(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(bookCatalogService
				.listCopies(publicId, pageableResolver.resolve(page, size)), bookCopyMapper::toResponse)));
	}

	@Override
	@PostMapping("/{publicId}/copies")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.BOOK_MANAGE)
	public ApiResponse<BookCopyResponse> addCopy(@PathVariable String publicId,
			@Valid @RequestBody AddBookCopyRequest request) {
		return ApiResponse.success(bookCopyMapper.toResponse(bookCatalogService.addCopy(publicId, request.copyCode())));
	}

	@Override
	@PatchMapping("/copies/{copyPublicId}/lost")
	@RequiresPermission(SchoolPermissions.BOOK_MANAGE)
	public ApiResponse<BookCopyResponse> markCopyLost(@PathVariable String copyPublicId) {
		return ApiResponse.success(bookCopyMapper.toResponse(bookCatalogService.markCopyLost(copyPublicId)));
	}

	@Override
	@PatchMapping("/copies/{copyPublicId}/damaged")
	@RequiresPermission(SchoolPermissions.BOOK_MANAGE)
	public ApiResponse<BookCopyResponse> markCopyDamaged(@PathVariable String copyPublicId) {
		return ApiResponse.success(bookCopyMapper.toResponse(bookCatalogService.markCopyDamaged(copyPublicId)));
	}
}
