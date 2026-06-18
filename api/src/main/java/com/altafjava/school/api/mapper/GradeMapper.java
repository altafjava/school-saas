package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GradeResponse;
import com.altafjava.school.domain.grade.model.Grade;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GradeMapper {

	@Mapping(target = "publicId", expression = "java(grade.getPublicId().toString())")
	GradeResponse toResponse(Grade grade);
}
