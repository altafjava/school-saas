package com.altafjava.school.api.controller;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.annotation.LastWriteWins;
import com.altafjava.school.api.controller.api.AdmissionSettingsApi;
import com.altafjava.school.api.dto.request.UpdateAdmissionSettingsRequest;
import com.altafjava.school.api.dto.response.AdmissionSettingsResponse;
import com.altafjava.school.application.service.AdmissionSettingsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admission-settings")
@RequiredArgsConstructor
public class AdmissionSettingsController implements AdmissionSettingsApi {

	private final AdmissionSettingsService admissionSettingsService;

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_FEE_MANAGE')")
	public ApiResponse<AdmissionSettingsResponse> get() {
		return ApiResponse.success(current());
	}

	@Override
	@PutMapping
	@LastWriteWins("A single tenant setting, with no versioned record behind it")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_FEE_MANAGE')")
	public ApiResponse<AdmissionSettingsResponse> update(@Valid @RequestBody UpdateAdmissionSettingsRequest request) {
		admissionSettingsService.setApplicationFee(request.applicationFeeAmount());
		return ApiResponse.success(current());
	}

	private AdmissionSettingsResponse current() {
		return new AdmissionSettingsResponse(admissionSettingsService.getApplicationFee().orElse(BigDecimal.ZERO));
	}
}
