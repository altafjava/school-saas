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
import com.altafjava.school.api.controller.api.FeeStructureApi;
import com.altafjava.school.api.dto.request.AssignFeeStructureRequest;
import com.altafjava.school.api.dto.request.ConfigureFeeAssignmentDueDateRequest;
import com.altafjava.school.api.dto.request.ConfigureFeeLateFeePolicyRequest;
import com.altafjava.school.api.dto.request.CreateFeeStructureRequest;
import com.altafjava.school.api.dto.request.ReviseFeeAmountRequest;
import com.altafjava.school.api.dto.response.FeeAssignmentResponse;
import com.altafjava.school.api.dto.response.FeeStructureResponse;
import com.altafjava.school.api.dto.response.FeeStructureRevisionResponse;
import com.altafjava.school.api.mapper.FeeAssignmentMapper;
import com.altafjava.school.api.mapper.FeeStructureMapper;
import com.altafjava.school.api.mapper.FeeStructureRevisionMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.FeeAssignmentService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.domain.fee.model.FeeFrequency;

@RestController
@RequestMapping("/api/v1/fee-structures")
public class FeeStructureController implements FeeStructureApi {

	private final FeeStructureService feeStructureService;
	private final FeeStructureMapper feeStructureMapper;
	private final FeeStructureRevisionMapper feeStructureRevisionMapper;
	private final FeeAssignmentService feeAssignmentService;
	private final FeeAssignmentMapper feeAssignmentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public FeeStructureController(FeeStructureService feeStructureService, FeeStructureMapper feeStructureMapper,
			FeeStructureRevisionMapper feeStructureRevisionMapper, FeeAssignmentService feeAssignmentService,
			FeeAssignmentMapper feeAssignmentMapper, SpringDataPageableResolver pageableResolver) {
		this.feeStructureService = feeStructureService;
		this.feeStructureMapper = feeStructureMapper;
		this.feeStructureRevisionMapper = feeStructureRevisionMapper;
		this.feeAssignmentService = feeAssignmentService;
		this.feeAssignmentMapper = feeAssignmentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	@SortableBy({ "name", "amount", "frequency" })
	public ApiResponse<com.altafjava.platform.core.model.Page<FeeStructureResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(feeStructureService.listFeeStructures(q, pageableResolver.resolve(page, size))
						.map(feeStructureMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<FeeStructureResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(feeStructureMapper.toResponse(feeStructureService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<FeeStructureResponse> create(@Valid @RequestBody CreateFeeStructureRequest request) {
		return ApiResponse.success(feeStructureMapper.toResponse(feeStructureService.create(
				request.name(),
				request.amount(),
				FeeFrequency.valueOf(request.frequency()),
				request.planType())));
	}

	@Override
	@PatchMapping("/{publicId}/amount")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<FeeStructureResponse> reviseAmount(@PathVariable String publicId,
			@Valid @RequestBody ReviseFeeAmountRequest request) {
		return ApiResponse
				.success(feeStructureMapper.toResponse(
						feeStructureService.reviseAmount(publicId, request.amount(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/late-fee-policy")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<FeeStructureResponse> configureLateFeePolicy(@PathVariable String publicId,
			@Valid @RequestBody ConfigureFeeLateFeePolicyRequest request) {
		return ApiResponse.success(feeStructureMapper.toResponse(
				feeStructureService.configureLateFeePolicy(publicId, request.graceDays(),
						request.lateFeePercentage(), request.expectedVersion())));
	}

	@Override
	@GetMapping("/{publicId}/revisions")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeeStructureRevisionResponse>> listRevisions(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(feeStructureService.listRevisions(publicId, pageableResolver.resolve(page, size))
						.map(feeStructureRevisionMapper::toResponse)));
	}

	@Override
	@PostMapping("/{publicId}/assignments")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<FeeAssignmentResponse> assign(@PathVariable String publicId,
			@Valid @RequestBody AssignFeeStructureRequest request) {
		return ApiResponse.success(feeAssignmentMapper.toResponse(
				feeAssignmentService.assign(publicId, request.studentPublicId(), request.classroomPublicId())));
	}

	@Override
	@GetMapping("/{publicId}/assignments")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeeAssignmentResponse>> listAssignments(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(feeAssignmentService.listForFeeStructure(publicId, pageableResolver.resolve(page, size))
						.map(feeAssignmentMapper::toResponse)));
	}

	@Override
	@PatchMapping("/{publicId}/assignments/{assignmentPublicId}/revoke")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<Void> revokeAssignment(@PathVariable String publicId, @PathVariable String assignmentPublicId) {
		feeAssignmentService.revoke(assignmentPublicId);
		return ApiResponse.success(null);
	}

	@Override
	@PatchMapping("/{publicId}/assignments/{assignmentPublicId}/due-date")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_STRUCTURE_MANAGE')")
	public ApiResponse<FeeAssignmentResponse> configureAssignmentDueDate(@PathVariable String publicId,
			@PathVariable String assignmentPublicId, @Valid @RequestBody ConfigureFeeAssignmentDueDateRequest request) {
		return ApiResponse
				.success(feeAssignmentMapper.toResponse(feeAssignmentService.configureDueDate(assignmentPublicId,
						request.dueDate(), request.graceDays(), request.lateFeePercentage(),
						request.expectedVersion())));
	}
}
