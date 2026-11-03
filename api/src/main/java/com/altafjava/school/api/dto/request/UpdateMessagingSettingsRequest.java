package com.altafjava.school.api.dto.request;

import java.time.LocalTime;

public record UpdateMessagingSettingsRequest(boolean enabled, LocalTime hoursStart, LocalTime hoursEnd) {
}
