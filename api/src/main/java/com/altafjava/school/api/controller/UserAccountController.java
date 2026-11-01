package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.UserAccountApi;
import com.altafjava.school.api.dto.request.CreateAccountRequest;
import com.altafjava.school.api.dto.response.AccountResponse;
import com.altafjava.school.application.service.UserAccountLinkService;
import com.altafjava.school.application.service.UserAccountLinkService.AccountRequest;
import com.altafjava.school.application.service.UserAccountLinkService.LinkedAccount;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserAccountController implements UserAccountApi {

	private final UserAccountLinkService userAccountLinkService;

	@Override
	@PostMapping("/students/{publicId}/account")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<AccountResponse> linkStudent(@PathVariable String publicId,
			@Valid @RequestBody CreateAccountRequest request) {
		return success(userAccountLinkService.linkStudent(publicId, toCommand(request)));
	}

	@Override
	@PostMapping("/employees/{publicId}/account")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<AccountResponse> linkEmployee(@PathVariable String publicId,
			@Valid @RequestBody CreateAccountRequest request) {
		return success(userAccountLinkService.linkEmployee(publicId, toCommand(request)));
	}

	@Override
	@PostMapping("/teachers/{publicId}/account")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<AccountResponse> linkTeacher(@PathVariable String publicId,
			@Valid @RequestBody CreateAccountRequest request) {
		return success(userAccountLinkService.linkTeacher(publicId, toCommand(request)));
	}

	private static AccountRequest toCommand(CreateAccountRequest request) {
		return new AccountRequest(request.userPublicId(), request.email(), request.password(), request.role());
	}

	private static ApiResponse<AccountResponse> success(LinkedAccount account) {
		return ApiResponse.success(new AccountResponse(account.userPublicId(), account.email()));
	}
}
