package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.GuardianSelfRegistrationApi;
import com.altafjava.school.api.dto.request.GuardianSelfRegisterRequest;
import com.altafjava.school.api.dto.response.GuardianResponse;
import com.altafjava.school.api.mapper.GuardianMapper;
import com.altafjava.school.application.service.GuardianSelfRegistrationService;

// SECURITY: permitAll() (unauthenticated), same shape as platform's AuthController#register — a
// guardian self-registering has no account yet, so it cannot present a JWT. Deliberately has no
// @PreAuthorize (matching AuthController/KNOWN_PUBLIC_CONTROLLERS convention for genuinely public
// endpoints), registered in KNOWN_PUBLIC_CONTROLLERS in ControllerAuthorizationFitnessTest, and
// reachable without a JWT through SchoolPlatformConfigurer.domainPublicPaths().
@RestController
@RequestMapping("/api/v1/guardians")
public class GuardianSelfRegistrationController implements GuardianSelfRegistrationApi {

	private final GuardianSelfRegistrationService guardianSelfRegistrationService;
	private final GuardianMapper guardianMapper;

	public GuardianSelfRegistrationController(GuardianSelfRegistrationService guardianSelfRegistrationService,
			GuardianMapper guardianMapper) {
		this.guardianSelfRegistrationService = guardianSelfRegistrationService;
		this.guardianMapper = guardianMapper;
	}

	@Override
	@PostMapping("/self-register")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<GuardianResponse> selfRegister(@Valid @RequestBody GuardianSelfRegisterRequest request) {
		return ApiResponse.success(guardianMapper.toResponse(guardianSelfRegistrationService.register(
				request.email(),
				request.password(),
				request.firstName(),
				request.lastName(),
				request.phone())));
	}
}
