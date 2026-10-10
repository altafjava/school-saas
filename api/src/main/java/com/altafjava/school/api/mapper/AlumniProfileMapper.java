package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AlumniProfileResponse;
import com.altafjava.school.domain.alumni.model.AlumniProfile;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AlumniProfileMapper {

	@Mapping(target = "publicId", expression = "java(profile.getPublicId().toString())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	AlumniProfileResponse toResponse(AlumniProfile profile);
}
