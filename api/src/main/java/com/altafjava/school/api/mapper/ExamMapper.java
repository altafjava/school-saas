package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ExamResponse;
import com.altafjava.school.domain.exam.model.Exam;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ExamMapper {

	@Mapping(target = "publicId", expression = "java(exam.getPublicId().toString())")
	ExamResponse toResponse(Exam exam);
}
