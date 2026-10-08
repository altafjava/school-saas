package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
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
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.school.api.controller.api.AdmissionApi;
import com.altafjava.school.api.dto.request.DecideAdmissionRequest;
import com.altafjava.school.api.dto.request.PublicAdmissionApplicationRequest;
import com.altafjava.school.api.dto.request.RecordEntranceTestScoreRequest;
import com.altafjava.school.api.dto.request.SubmitAdmissionRequest;
import com.altafjava.school.api.dto.request.WaiveApplicationFeeRequest;
import com.altafjava.school.api.dto.response.AdmissionResponse;
import com.altafjava.school.api.mapper.AdmissionMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.domain.admission.model.DecisionOutcome;

/**
 * All endpoints here require {@code TENANT_ADMIN} except {@link #apply}, which is intended to be
 * unauthenticated (a prospective guardian applying before any account exists) — see that method's
 * Javadoc for the current, incomplete state of that intent.
 * <p>
 * {@code @PreAuthorize} is declared per method rather than at the class level (matching
 * {@code AttendanceController}/{@code ExamController}/{@code FeeStructureController}/
 * {@code GuardianController}'s convention) specifically so {@link #apply} can be exempted —
 * Spring Security method security does not let a method-level annotation override/remove a
 * class-level one on the same class.
 */
@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionController implements AdmissionApi {

	private final AdmissionService admissionService;
	private final AdmissionMapper admissionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AdmissionController(AdmissionService admissionService, AdmissionMapper admissionMapper,
			SpringDataPageableResolver pageableResolver) {
		this.admissionService = admissionService;
		this.admissionMapper = admissionMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	@SortableBy({ "applicantFirstName", "applicantLastName", "appliedGrade", "status", "submittedAt" })
	public ApiResponse<com.altafjava.platform.core.model.Page<AdmissionResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(admissionService.listAdmissions(pageableResolver.resolve(page, size))
						.map(admissionMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public ApiResponse<AdmissionResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(admissionMapper.toResponse(admissionService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<AdmissionResponse> submit(@Valid @RequestBody SubmitAdmissionRequest request) {
		return ApiResponse.success(admissionMapper.toResponse(admissionService.submit(
				request.applicantFirstName(),
				request.applicantLastName(),
				request.applicantDateOfBirth(),
				request.guardianFirstName(),
				request.guardianLastName(),
				request.guardianEmail(),
				request.guardianPhone(),
				request.appliedGrade())));
	}

	@Override
	@PostMapping("/apply")
	@ResponseStatus(HttpStatus.CREATED)
	@RequireIdempotencyKey
	public ApiResponse<AdmissionResponse> apply(@Valid @RequestBody PublicAdmissionApplicationRequest request) {
		return ApiResponse.success(admissionMapper.toResponse(admissionService.submit(
				request.applicantFirstName(),
				request.applicantLastName(),
				request.applicantDateOfBirth(),
				request.guardianFirstName(),
				request.guardianLastName(),
				request.guardianEmail(),
				request.guardianPhone(),
				request.appliedGrade())));
	}

	@Override
	@PatchMapping("/{publicId}/under-review")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public ApiResponse<AdmissionResponse> markUnderReview(@PathVariable String publicId) {
		return ApiResponse.success(admissionMapper.toResponse(admissionService.markUnderReview(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/decision")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<AdmissionResponse> decide(@PathVariable String publicId,
			@Valid @RequestBody DecideAdmissionRequest request) {
		if (request.outcome() != DecisionOutcome.APPROVED) {
			return ApiResponse.success(admissionMapper
					.toResponse(admissionService.reject(publicId, request.decidedBy(), request.notes())));
		}
		if (!StringUtils.hasText(request.studentCode())) {
			throw new BusinessException("studentCode is required when approving an admission");
		}
		return ApiResponse.success(admissionMapper.toResponse(admissionService.requestApproval(publicId,
				request.decidedBy(), request.notes(), request.studentCode())));
	}

	@Override
	@PostMapping("/{publicId}/entrance-test-score")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public ApiResponse<AdmissionResponse> recordEntranceTestScore(@PathVariable String publicId,
			@Valid @RequestBody RecordEntranceTestScoreRequest request) {
		return ApiResponse.success(admissionMapper.toResponse(
				admissionService.recordEntranceTestScore(publicId, request.score(), request.maxScore())));
	}

	@Override
	@PostMapping("/merit-list")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public ApiResponse<List<AdmissionResponse>> generateMeritList(
			@RequestParam String appliedGrade,
			@RequestParam int availableSeats) {
		return ApiResponse.success(admissionService.generateMeritList(appliedGrade, availableSeats).stream()
				.map(admissionMapper::toResponse)
				.toList());
	}

	@Override
	@PatchMapping("/{publicId}/promote-from-waitlist")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public ApiResponse<AdmissionResponse> promoteFromWaitlist(@PathVariable String publicId) {
		return ApiResponse.success(admissionMapper.toResponse(admissionService.promoteFromWaitlist(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/application-fee/payment")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_FEE_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<AdmissionResponse> recordApplicationFee(@PathVariable String publicId) {
		return ApiResponse.success(admissionMapper.toResponse(admissionService.recordApplicationFeePayment(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/application-fee/waive")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_FEE_MANAGE')")
	public ApiResponse<AdmissionResponse> waiveApplicationFee(@PathVariable String publicId,
			@Valid @RequestBody WaiveApplicationFeeRequest request) {
		return ApiResponse
				.success(admissionMapper.toResponse(admissionService.waiveApplicationFee(publicId, request.reason())));
	}

	@Override
	@PostMapping("/{publicId}/offer-letter")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<AdmissionResponse> issueOfferLetter(@PathVariable String publicId) {
		return ApiResponse.success(admissionMapper.toResponse(admissionService.issueOfferLetter(publicId)));
	}

	// Not ApiResponse-wrapped: a PDF body with its own Content-Type, like every download endpoint.
	@Override
	@GetMapping("/{publicId}/offer-letter/download")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public ResponseEntity<byte[]> downloadOfferLetter(@PathVariable String publicId) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("offer-letter-" + publicId + ".pdf").build()
								.toString())
				.body(admissionService.downloadOfferLetter(publicId));
	}
}
