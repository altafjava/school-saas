package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CheckInVisitorRequest(@NotBlank String visitorRequestPublicId, String photoFilePublicId) {
}
