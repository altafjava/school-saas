package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.TermResponse;
import com.altafjava.school.domain.term.model.Term;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TermMapper {

	@Mapping(target = "publicId", expression = "java(term.getPublicId().toString())")
	@Mapping(target = "current", source = "current")
	@Mapping(target = "academicYearPublicId", source = "academicYearId", qualifiedByName = "academicYear")
	TermResponse toResponse(Term term);
}
