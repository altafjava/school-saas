package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ClassroomResponse;
import com.altafjava.school.domain.classroom.model.Classroom;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClassroomMapper {

	@Mapping(target = "publicId", expression = "java(classroom.getPublicId().toString())")
	ClassroomResponse toResponse(Classroom classroom);
}
