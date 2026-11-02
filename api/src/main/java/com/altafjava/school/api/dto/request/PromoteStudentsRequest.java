package com.altafjava.school.api.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.classroom.model.PromotionOutcome;

public record PromoteStudentsRequest(
		String targetClassroomPublicId,
		@NotEmpty @Size(max = 500) List<@Valid PromotionEntry> entries) {

	public PromoteStudentsRequest {
		entries = entries == null ? null : List.copyOf(entries);
	}

	public record PromotionEntry(
			@NotBlank String studentPublicId,
			@NotNull PromotionOutcome outcome,
			String targetClassroomPublicId,
			@Size(max = 500) String reason) {
	}
}
