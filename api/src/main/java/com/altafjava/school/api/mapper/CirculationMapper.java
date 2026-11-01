package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CirculationResponse;
import com.altafjava.school.domain.library.model.Circulation;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CirculationMapper {

	@Mapping(target = "publicId", expression = "java(circulation.getPublicId().toString())")
	@Mapping(target = "bookCopyPublicId", source = "bookCopyId", qualifiedByName = "bookCopy")
	@Mapping(target = "bookCopy", source = "bookCopyId", qualifiedByName = "bookCopySummary")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "student", source = "studentId", qualifiedByName = "studentSummary")
	CirculationResponse toResponse(Circulation circulation);
}
