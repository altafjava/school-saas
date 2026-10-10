package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.EventRegistrationResponse;
import com.altafjava.school.domain.event.model.EventRegistration;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EventRegistrationMapper {

	@Mapping(target = "publicId", expression = "java(registration.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(registration.getStatus().name())")
	@Mapping(target = "eventPublicId", source = "eventId", qualifiedByName = "event")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	EventRegistrationResponse toResponse(EventRegistration registration);
}
