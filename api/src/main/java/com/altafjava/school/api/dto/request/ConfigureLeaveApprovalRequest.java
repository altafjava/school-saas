package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigureLeaveApprovalRequest(@NotNull @Min(1) @Max(2) Integer approvalLevels) {
}
