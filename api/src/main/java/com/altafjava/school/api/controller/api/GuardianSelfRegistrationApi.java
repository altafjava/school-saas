package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.GuardianSelfRegisterRequest;
import com.altafjava.school.api.dto.response.GuardianResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Guardian Self Registration", description = "Unauthenticated (permitAll) self-registration for a "
		+ "guardian who has no account yet, mirroring platform's AuthController#register — access to any "
		+ "student/tenant data must be granted separately afterward, this endpoint never accepts caller-supplied roles.")
public interface GuardianSelfRegistrationApi {

	@Operation(summary = "Self register")
	public ApiResponse<GuardianResponse> selfRegister(@Valid @RequestBody GuardianSelfRegisterRequest request);
}
