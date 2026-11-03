package com.altafjava.school.api.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A guardian names {@code teacherPublicId}; a teacher names {@code guardianPublicId}. */
public record StartConversationRequest(
		@NotBlank String studentPublicId,
		String teacherPublicId,
		String guardianPublicId,
		@NotBlank @Size(max = 4000) String body,
		@Size(max = 5) List<@NotBlank String> attachmentFilePublicIds) {
}
