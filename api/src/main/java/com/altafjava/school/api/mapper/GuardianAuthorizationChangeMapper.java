package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GuardianAuthorizationChangeResponse;
import com.altafjava.school.domain.guardian.model.GuardianAuthorizationChange;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GuardianAuthorizationChangeMapper {

	@Mapping(target = "publicId", expression = "java(change.getPublicId().toString())")
	@Mapping(target = "changedAt", source = "createdAt")
	@Mapping(target = "guardianPublicId", source = "guardianId", qualifiedByName = "guardian")
	@Mapping(target = "changedByUserPublicId", source = "changedByUserId", qualifiedByName = "user")
	GuardianAuthorizationChangeResponse toResponse(GuardianAuthorizationChange change);
}
