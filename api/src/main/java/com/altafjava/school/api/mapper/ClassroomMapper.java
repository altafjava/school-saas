package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ClassroomResponse;
import com.altafjava.school.domain.classroom.model.Classroom;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClassroomMapper {

	@Mapping(target = "publicId", expression = "java(classroom.getPublicId().toString())")
	@Mapping(target = "classTeacherPublicId", source = "classTeacherId", qualifiedByName = "employee")
	@Mapping(target = "classTeacher", source = "classTeacherId", qualifiedByName = "employeeSummary")
	@Mapping(target = "curriculumPublicId", source = "curriculumId", qualifiedByName = "curriculum")
	@Mapping(target = "curriculum", source = "curriculumId", qualifiedByName = "curriculumSummary")
	ClassroomResponse toResponse(Classroom classroom);
}
