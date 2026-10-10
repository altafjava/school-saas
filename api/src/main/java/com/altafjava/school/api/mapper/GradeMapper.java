package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GradeResponse;
import com.altafjava.school.domain.grade.model.Grade;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GradeMapper {

	@Mapping(target = "publicId", expression = "java(grade.getPublicId().toString())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "subjectPublicId", source = "subjectId", qualifiedByName = "subject")
	@Mapping(target = "examPublicId", source = "examId", qualifiedByName = "exam")
	GradeResponse toResponse(Grade grade);
}
