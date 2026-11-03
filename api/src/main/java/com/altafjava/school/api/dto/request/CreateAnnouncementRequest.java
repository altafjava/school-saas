package com.altafjava.school.api.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.announcement.model.AnnouncementChannel;
import com.altafjava.school.domain.announcement.model.AudienceType;

public record CreateAnnouncementRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 20000) String body,
		@NotNull AudienceType audienceType,
		@Size(max = 500) Set<@NotBlank @Size(max = 100) String> targets,
		boolean includeStudents,
		@NotEmpty Set<AnnouncementChannel> channels,
		@Size(max = 5) List<@NotBlank String> attachmentFilePublicIds,
		Instant expiresAt) {
}
