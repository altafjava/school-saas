package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CurriculumResponse;
import com.altafjava.school.domain.curriculum.model.Curriculum;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CurriculumMapper {

	@Mapping(target = "publicId", expression = "java(curriculum.getPublicId().toString())")
	@Mapping(target = "boardPublicId", source = "boardId", qualifiedByName = "board")
	@Mapping(target = "gradingScalePublicId", source = "gradingScaleId", qualifiedByName = "gradingScale")
	CurriculumResponse toResponse(Curriculum curriculum);
}
