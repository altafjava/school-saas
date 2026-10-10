package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignTicketRequest(@NotBlank String assignedToUserPublicId) {
}
