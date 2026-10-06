package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.api.dto.response.IssuedDocumentResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IssuedDocumentMapper {

	@Mapping(target = "publicId", expression = "java(issuance.getPublicId().toString())")
	@Mapping(target = "revoked", expression = "java(issuance.isRevoked())")
	IssuedDocumentResponse toResponse(DocumentIssuance issuance);
}
