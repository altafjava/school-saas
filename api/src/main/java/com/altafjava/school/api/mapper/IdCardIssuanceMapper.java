package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.api.dto.response.IdCardIssuanceResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IdCardIssuanceMapper {

	@Mapping(target = "publicId", expression = "java(issuance.getPublicId().toString())")
	IdCardIssuanceResponse toResponse(DocumentIssuance issuance);
}
