package com.altafjava.school.api.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.AssignEnquiryRequest;
import com.altafjava.school.api.dto.request.CloseEnquiryRequest;
import com.altafjava.school.api.dto.request.ConvertEnquiryRequest;
import com.altafjava.school.api.dto.request.ReceiveEnquiryRequest;
import com.altafjava.school.api.dto.request.RecordFollowUpRequest;
import com.altafjava.school.api.dto.response.enquiry.EnquiryFollowUpResponse;
import com.altafjava.school.api.dto.response.enquiry.EnquiryResponse;
import com.altafjava.school.api.mapper.EnquiryMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.enquiry.EnquiryFilter;
import com.altafjava.school.application.enquiry.EnquiryService;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.domain.enquiry.model.EnquiryDetails;
import com.altafjava.school.domain.enquiry.model.EnquirySource;
import com.altafjava.school.domain.enquiry.model.EnquiryStatus;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Enquiries", description = "Admission enquiries, their follow-ups, and conversion into applications.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
@RestController
@RequestMapping("/api/v1/enquiries")
@RequiredArgsConstructor
public class EnquiryController {

	private final EnquiryService enquiryService;
	private final EnquiryMapper enquiryMapper;
	private final SpringDataPageableResolver pageableResolver;
	private final ReferenceSummaries referenceSummaries;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@Operation(summary = "Receive an enquiry")
	public ApiResponse<EnquiryResponse> receive(@Valid @RequestBody ReceiveEnquiryRequest request) {
		return ApiResponse.success(enquiryMapper.toResponse(enquiryService.receive(
				new EnquiryDetails(request.childFirstName(), request.childLastName(), request.childDateOfBirth(),
						request.gradeSought(), request.guardianFirstName(), request.guardianLastName(),
						request.guardianEmail(), request.guardianPhone()),
				request.source(), request.notes(), request.assignedEmployeePublicId())));
	}

	@GetMapping
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@SortableBy({ "createdAt", "nextFollowUpOn", "status", "gradeSought" })
	@Operation(summary = "Enquiries", description = "Every filter is optional. followUpDueBy lists open enquiries due "
			+ "a follow-up on or before that date; q matches the child's or guardian's name.")
	public ApiResponse<com.altafjava.platform.core.model.Page<EnquiryResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) EnquiryStatus status,
			@RequestParam(required = false) EnquirySource source,
			@RequestParam(required = false) String assignedEmployeePublicId,
			@RequestParam(required = false) LocalDate followUpDueBy,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				enquiryService.list(new EnquiryFilter(status, source, assignedEmployeePublicId, followUpDueBy, q),
						pageableResolver.resolve(page, size)),
				enquiryMapper::toResponse)));
	}

	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@Operation(summary = "Enquiry")
	public ApiResponse<EnquiryResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(enquiryMapper.toResponse(enquiryService.find(publicId)));
	}

	@PutMapping("/{publicId}/assignee")
	@Command
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@Operation(summary = "Assign", description = "Hands an open enquiry to an active staff member.")
	public ApiResponse<EnquiryResponse> assign(@PathVariable String publicId,
			@Valid @RequestBody AssignEnquiryRequest request) {
		return ApiResponse
				.success(enquiryMapper.toResponse(enquiryService.assign(publicId, request.employeePublicId())));
	}

	@PostMapping("/{publicId}/follow-ups")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@Operation(summary = "Record a follow-up", description = "The first follow-up takes a new enquiry into progress.")
	public ApiResponse<EnquiryFollowUpResponse> recordFollowUp(@PathVariable String publicId,
			@Valid @RequestBody RecordFollowUpRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(enquiryMapper.toResponse(enquiryService.recordFollowUp(publicId, request.channel(),
				request.note(), request.nextFollowUpOn(), user.getId())));
	}

	@GetMapping("/{publicId}/follow-ups")
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@SortableBy({ "followedUpAt" })
	@Operation(summary = "Follow-ups")
	public ApiResponse<com.altafjava.platform.core.model.Page<EnquiryFollowUpResponse>> followUps(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				enquiryService.followUps(publicId, pageableResolver.resolve(page, size))
						.map(enquiryMapper::toResponse)));
	}

	@PostMapping("/{publicId}/close")
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@Operation(summary = "Close")
	public ApiResponse<EnquiryResponse> close(@PathVariable String publicId,
			@Valid @RequestBody CloseEnquiryRequest request) {
		return ApiResponse.success(enquiryMapper.toResponse(enquiryService.close(publicId, request.reason())));
	}

	@PostMapping("/{publicId}/reopen")
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@Operation(summary = "Reopen")
	public ApiResponse<EnquiryResponse> reopen(@PathVariable String publicId) {
		return ApiResponse.success(enquiryMapper.toResponse(enquiryService.reopen(publicId)));
	}

	@PostMapping("/{publicId}/convert")
	@RequiresPermission(SchoolPermissions.ENQUIRY_MANAGE)
	@Operation(summary = "Convert into an application", description = "Submits an admission application with the "
			+ "family's details from the enquiry, and links the two.")
	public ApiResponse<EnquiryResponse> convert(@PathVariable String publicId,
			@Valid @RequestBody ConvertEnquiryRequest request) {
		return ApiResponse.success(enquiryMapper.toResponse(enquiryService.convert(publicId,
				new EnquiryService.Conversion(request.childDateOfBirth(), request.childGender()))));
	}
}
