package com.altafjava.school.api.dto.response;

import java.time.LocalTime;

public record MessagingSettingsResponse(boolean enabled, LocalTime hoursStart, LocalTime hoursEnd) {
}
