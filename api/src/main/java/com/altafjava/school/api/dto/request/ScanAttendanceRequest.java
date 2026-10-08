package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScanAttendanceRequest(@NotBlank @Size(max = 2048) String payload) {
}
