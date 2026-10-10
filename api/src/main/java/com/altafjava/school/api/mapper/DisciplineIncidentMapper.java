package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.DisciplineIncidentResponse;
import com.altafjava.school.domain.discipline.model.DisciplineIncident;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DisciplineIncidentMapper {

	@Mapping(target = "publicId", expression = "java(incident.getPublicId().toString())")
	@Mapping(target = "severity", expression = "java(incident.getSeverity().name())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "reportedByTeacherPublicId", source = "reportedByTeacherId", qualifiedByName = "employee")
	DisciplineIncidentResponse toResponse(DisciplineIncident incident);
}
