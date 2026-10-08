package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.VenueResponse;
import com.altafjava.school.domain.timetable.model.Venue;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VenueMapper {

	@Mapping(target = "publicId", expression = "java(venue.getPublicId().toString())")
	@Mapping(target = "venueType", expression = "java(venue.getVenueType().name())")
	VenueResponse toResponse(Venue venue);
}
