package com.altafjava.school.api.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
		@NotBlank @Size(max = 4000) String body,
		@Size(max = 5) List<@NotBlank String> attachmentFilePublicIds) {
}
