package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateAccountRequest;
import com.altafjava.school.api.dto.response.AccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Accounts", description = "Logins for students and staff")
public interface UserAccountApi {

	@Operation(summary = "Create or link a student's login")
	ApiResponse<AccountResponse> linkStudent(@PathVariable String publicId,
			@Valid @RequestBody CreateAccountRequest request);

	@Operation(summary = "Create or link an employee's login")
	ApiResponse<AccountResponse> linkEmployee(@PathVariable String publicId,
			@Valid @RequestBody CreateAccountRequest request);

	@Operation(summary = "Create or link a teacher's login")
	ApiResponse<AccountResponse> linkTeacher(@PathVariable String publicId,
			@Valid @RequestBody CreateAccountRequest request);
}
