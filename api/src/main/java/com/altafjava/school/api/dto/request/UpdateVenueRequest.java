package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.timetable.model.VenueType;

public record UpdateVenueRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull VenueType venueType,
		@Positive Integer capacity) {
}
