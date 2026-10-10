package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignExamTermRequest(@NotBlank String termPublicId) {
}
