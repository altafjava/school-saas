package com.altafjava.school.api.dto.request;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.announcement.model.AudienceType;

public record SendEmergencyAlertRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 20000) String body,
		@NotNull AudienceType audienceType,
		@Size(max = 500) Set<@NotBlank @Size(max = 100) String> targets,
		boolean includeStudents) {
}
