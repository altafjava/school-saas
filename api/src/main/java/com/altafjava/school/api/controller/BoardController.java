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
import com.altafjava.school.api.controller.api.BoardApi;
import com.altafjava.school.api.dto.request.CreateBoardRequest;
import com.altafjava.school.api.dto.request.UpdateBoardRequest;
import com.altafjava.school.api.dto.response.BoardResponse;
import com.altafjava.school.api.mapper.BoardMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.BoardService;

@RestController
@RequestMapping("/api/v1/boards")
public class BoardController implements BoardApi {

	private final BoardService boardService;
	private final BoardMapper boardMapper;

	private final SpringDataPageableResolver pageableResolver;

	public BoardController(BoardService boardService, BoardMapper boardMapper,
			SpringDataPageableResolver pageableResolver) {
		this.boardService = boardService;
		this.boardMapper = boardMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('BOARD_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<BoardResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(boardService.list(pageableResolver.resolve(page, size)).map(boardMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('BOARD_READ')")
	public ApiResponse<BoardResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(boardMapper.toResponse(boardService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('BOARD_WRITE')")
	public ApiResponse<BoardResponse> create(@Valid @RequestBody CreateBoardRequest request) {
		return ApiResponse.success(
				boardMapper.toResponse(boardService.create(request.name(), request.code(), request.description())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('BOARD_WRITE')")
	public ApiResponse<BoardResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateBoardRequest request) {
		return ApiResponse.success(boardMapper.toResponse(
				boardService.updateDetails(publicId, request.name(), request.code(), request.description(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('BOARD_WRITE')")
	public ApiResponse<BoardResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(boardMapper.toResponse(boardService.deactivate(publicId)));
	}
}
