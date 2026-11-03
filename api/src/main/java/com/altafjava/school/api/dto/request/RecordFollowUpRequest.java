package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.enquiry.model.FollowUpChannel;

public record RecordFollowUpRequest(@NotNull FollowUpChannel channel, @NotBlank @Size(max = 2000) String note,
		LocalDate nextFollowUpOn) {
}
