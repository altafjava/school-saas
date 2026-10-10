package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.StudentGuardianLinkResponse;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StudentGuardianLinkMapper {

	@Mapping(target = "publicId", expression = "java(link.getPublicId().toString())")
	@Mapping(target = "relationshipType", expression = "java(link.getRelationshipType().name())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "guardianPublicId", source = "guardianId", qualifiedByName = "guardian")
	StudentGuardianLinkResponse toResponse(StudentGuardianLink link);
}
