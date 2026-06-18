package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.TeacherResponse;
import com.altafjava.school.domain.teacher.model.Teacher;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TeacherMapper {

	@Mapping(target = "publicId", expression = "java(teacher.getPublicId().toString())")
	TeacherResponse toResponse(Teacher teacher);
}
