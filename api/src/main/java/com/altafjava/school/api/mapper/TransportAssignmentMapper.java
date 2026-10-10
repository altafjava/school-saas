package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.TransportAssignmentResponse;
import com.altafjava.school.domain.transport.model.TransportAssignment;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransportAssignmentMapper {

	@Mapping(target = "publicId", expression = "java(assignment.getPublicId().toString())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "routePublicId", source = "routeId", qualifiedByName = "route")
	@Mapping(target = "vehiclePublicId", source = "vehicleId", qualifiedByName = "vehicle")
	@Mapping(target = "routeStopPublicId", source = "routeStopId", qualifiedByName = "routeStop")
	TransportAssignmentResponse toResponse(TransportAssignment assignment);
}
