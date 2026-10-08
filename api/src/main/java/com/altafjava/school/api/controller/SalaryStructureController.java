package com.altafjava.school.api.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import com.altafjava.school.api.controller.api.SalaryStructureApi;
import com.altafjava.school.api.dto.request.CreateSalaryStructureRequest;
import com.altafjava.school.api.dto.request.PayComponentAmountRequest;
import com.altafjava.school.api.dto.request.SupersedeSalaryStructureRequest;
import com.altafjava.school.api.dto.response.SalaryStructureResponse;
import com.altafjava.school.api.mapper.SalaryStructureMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.SalaryStructureService;

@RestController
@RequestMapping("/api/v1/salary-structures")
public class SalaryStructureController implements SalaryStructureApi {

	private final SalaryStructureService salaryStructureService;
	private final SalaryStructureMapper salaryStructureMapper;

	private final SpringDataPageableResolver pageableResolver;

	public SalaryStructureController(SalaryStructureService salaryStructureService,
			SalaryStructureMapper salaryStructureMapper, SpringDataPageableResolver pageableResolver) {
		this.salaryStructureService = salaryStructureService;
		this.salaryStructureMapper = salaryStructureMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SALARY_STRUCTURE_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<SalaryStructureResponse>> listForEmployee(
			@RequestParam String employeePublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				salaryStructureService.listForEmployee(employeePublicId, pageableResolver.resolve(page, size))
						.map(salaryStructureMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SALARY_STRUCTURE_MANAGE')")
	public ApiResponse<SalaryStructureResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(salaryStructureMapper.toResponse(salaryStructureService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SALARY_STRUCTURE_MANAGE')")
	public ApiResponse<SalaryStructureResponse> create(@Valid @RequestBody CreateSalaryStructureRequest request) {
		return ApiResponse
				.success(salaryStructureMapper.toResponse(salaryStructureService.create(request.employeePublicId(),
						toAmountsByCode(request.components()), request.effectiveFrom())));
	}

	// Narrow PATCH: the current active structure is superseded by a new one (never edited in
	// place), mirroring SalaryStructureService's one-active-per-employee invariant.
	@Override
	@PatchMapping("/{publicId}/supersede")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SALARY_STRUCTURE_MANAGE')")
	public ApiResponse<SalaryStructureResponse> supersede(@PathVariable String publicId,
			@Valid @RequestBody SupersedeSalaryStructureRequest request) {
		return ApiResponse.success(salaryStructureMapper.toResponse(salaryStructureService.supersede(publicId,
				toAmountsByCode(request.components()), request.effectiveFrom())));
	}

	// LinkedHashMap explicitly: preserves the caller's component ordering through to persistence
	// and the response, rather than Collectors.toMap's unspecified (HashMap) iteration order.
	private Map<String, BigDecimal> toAmountsByCode(List<PayComponentAmountRequest> components) {
		return components.stream()
				.collect(Collectors.toMap(PayComponentAmountRequest::code, PayComponentAmountRequest::amount,
						(a, b) -> a, java.util.LinkedHashMap::new));
	}
}
