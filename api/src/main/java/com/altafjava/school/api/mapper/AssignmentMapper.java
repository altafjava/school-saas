package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AssignmentResponse;
import com.altafjava.school.domain.lms.model.Assignment;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AssignmentMapper {

	@Mapping(target = "publicId", expression = "java(assignment.getPublicId().toString())")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	@Mapping(target = "subjectPublicId", source = "subjectId", qualifiedByName = "subject")
	@Mapping(target = "teacherPublicId", source = "teacherId", qualifiedByName = "employee")
	AssignmentResponse toResponse(Assignment assignment);
}
