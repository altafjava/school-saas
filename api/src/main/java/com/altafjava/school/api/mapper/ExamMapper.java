package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ExamResponse;
import com.altafjava.school.domain.exam.model.Exam;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ExamMapper {

	@Mapping(target = "publicId", expression = "java(exam.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(exam.getStatus().name())")
	@Mapping(target = "resultsPublished", expression = "java(exam.isResultsPublished())")
	@Mapping(target = "subjectPublicId", source = "subjectId", qualifiedByName = "subject")
	@Mapping(target = "subject", source = "subjectId", qualifiedByName = "subjectSummary")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	@Mapping(target = "classroom", source = "classroomId", qualifiedByName = "classroomSummary")
	@Mapping(target = "termPublicId", source = "termId", qualifiedByName = "term")
	@Mapping(target = "term", source = "termId", qualifiedByName = "termSummary")
	@Mapping(target = "examTypePublicId", source = "examTypeId", qualifiedByName = "examType")
	@Mapping(target = "examType", source = "examTypeId", qualifiedByName = "examTypeSummary")
	ExamResponse toResponse(Exam exam);
}
