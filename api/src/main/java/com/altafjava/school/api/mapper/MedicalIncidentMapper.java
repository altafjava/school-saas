package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.MedicalIncidentResponse;
import com.altafjava.school.domain.health.model.MedicalIncident;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MedicalIncidentMapper {

	@Mapping(target = "publicId", expression = "java(incident.getPublicId().toString())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "recordedByUserPublicId", source = "recordedByUserId", qualifiedByName = "user")
	MedicalIncidentResponse toResponse(MedicalIncident incident);
}
