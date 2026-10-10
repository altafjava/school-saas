package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ClassroomResponse;
import com.altafjava.school.domain.classroom.model.Classroom;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClassroomMapper {

	@Mapping(target = "publicId", expression = "java(classroom.getPublicId().toString())")
	@Mapping(target = "classTeacherPublicId", source = "classTeacherId", qualifiedByName = "employee")
	@Mapping(target = "curriculumPublicId", source = "curriculumId", qualifiedByName = "curriculum")
	ClassroomResponse toResponse(Classroom classroom);
}
