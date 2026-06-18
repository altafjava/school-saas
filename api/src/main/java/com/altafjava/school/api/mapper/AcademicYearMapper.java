package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AcademicYearResponse;
import com.altafjava.school.domain.academicyear.model.AcademicYear;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AcademicYearMapper {

	@Mapping(target = "publicId", expression = "java(academicYear.getPublicId().toString())")
	@Mapping(target = "current", source = "current")
	AcademicYearResponse toResponse(AcademicYear academicYear);
}
