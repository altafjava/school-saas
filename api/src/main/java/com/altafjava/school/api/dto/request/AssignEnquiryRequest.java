package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignEnquiryRequest(@NotBlank String employeePublicId) {
}
