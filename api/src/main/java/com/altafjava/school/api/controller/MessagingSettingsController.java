package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.annotation.LastWriteWins;
import com.altafjava.school.api.controller.api.MessagingSettingsApi;
import com.altafjava.school.api.dto.request.UpdateMessagingSettingsRequest;
import com.altafjava.school.api.dto.response.MessagingSettingsResponse;
import com.altafjava.school.application.messaging.MessagingSettings;
import com.altafjava.school.application.messaging.MessagingSettingsService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/messaging-settings")
@RequiredArgsConstructor
public class MessagingSettingsController implements MessagingSettingsApi {

	private final MessagingSettingsService settingsService;

	@Override
	@GetMapping
	@RequiresPermission({ SchoolPermissions.MESSAGING_SETTINGS_MANAGE, SchoolPermissions.MESSAGING_PARTICIPATE })
	public ApiResponse<MessagingSettingsResponse> get() {
		return ApiResponse.success(toResponse(settingsService.get()));
	}

	@Override
	@PutMapping
	@LastWriteWins("A single switch and window, set whole by one administrator")
	@RequiresPermission(SchoolPermissions.MESSAGING_SETTINGS_MANAGE)
	public ApiResponse<MessagingSettingsResponse> update(@Valid @RequestBody UpdateMessagingSettingsRequest request) {
		return ApiResponse.success(toResponse(settingsService
				.update(new MessagingSettings(request.enabled(), request.hoursStart(), request.hoursEnd()))));
	}

	private MessagingSettingsResponse toResponse(MessagingSettings settings) {
		return new MessagingSettingsResponse(settings.enabled(), settings.hoursStart(), settings.hoursEnd());
	}
}
