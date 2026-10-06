package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GuardianAuthorizationChangeResponse;
import com.altafjava.school.domain.guardian.model.GuardianAuthorizationChange;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GuardianAuthorizationChangeMapper {

	@Mapping(target = "publicId", expression = "java(change.getPublicId().toString())")
	@Mapping(target = "changedAt", source = "createdAt")
	GuardianAuthorizationChangeResponse toResponse(GuardianAuthorizationChange change);
}
