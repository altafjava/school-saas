package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CurriculumResponse;
import com.altafjava.school.domain.curriculum.model.Curriculum;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CurriculumMapper {

	@Mapping(target = "publicId", expression = "java(curriculum.getPublicId().toString())")
	@Mapping(target = "boardPublicId", source = "boardId", qualifiedByName = "board")
	@Mapping(target = "board", source = "boardId", qualifiedByName = "boardSummary")
	@Mapping(target = "gradingScalePublicId", source = "gradingScaleId", qualifiedByName = "gradingScale")
	@Mapping(target = "gradingScale", source = "gradingScaleId", qualifiedByName = "gradingScaleSummary")
	CurriculumResponse toResponse(Curriculum curriculum);
}
