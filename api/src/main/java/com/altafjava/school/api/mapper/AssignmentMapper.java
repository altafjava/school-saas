package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AssignmentResponse;
import com.altafjava.school.domain.lms.model.Assignment;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AssignmentMapper {

	@Mapping(target = "publicId", expression = "java(assignment.getPublicId().toString())")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	@Mapping(target = "classroom", source = "classroomId", qualifiedByName = "classroomSummary")
	@Mapping(target = "subjectPublicId", source = "subjectId", qualifiedByName = "subject")
	@Mapping(target = "subject", source = "subjectId", qualifiedByName = "subjectSummary")
	@Mapping(target = "teacherPublicId", source = "teacherId", qualifiedByName = "employee")
	@Mapping(target = "teacher", source = "teacherId", qualifiedByName = "employeeSummary")
	AssignmentResponse toResponse(Assignment assignment);
}
