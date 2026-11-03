package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.UpdateMessagingSettingsRequest;
import com.altafjava.school.api.dto.response.MessagingSettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Messaging Settings", description = "A school's messaging switch and hours, in the school's own timezone.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface MessagingSettingsApi {

	@Operation(summary = "Get")
	public ApiResponse<MessagingSettingsResponse> get();

	@Operation(summary = "Update", description = "Leave both hours empty for messaging open all day.")
	public ApiResponse<MessagingSettingsResponse> update(@Valid @RequestBody UpdateMessagingSettingsRequest request);
}
