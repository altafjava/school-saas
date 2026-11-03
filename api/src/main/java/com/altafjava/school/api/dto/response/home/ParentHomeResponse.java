package com.altafjava.school.api.dto.response.home;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The parent home screen.")
public record ParentHomeResponse(@Schema(description = "Today at the school") LocalDate date,
		@Schema(description = "False on Sundays and holidays") boolean schoolDay,
		@Schema(description = "One summary per linked child") List<StudentOverviewResponse> children,
		InboxResponse inbox) {

	public ParentHomeResponse {
		children = children == null ? null : List.copyOf(children);
	}
}
