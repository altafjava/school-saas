package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CertificateTypeResponse;
import com.altafjava.school.domain.certificate.model.CertificateType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CertificateTypeMapper {

	@Mapping(target = "publicId", expression = "java(type.getPublicId().toString())")
	@Mapping(target = "documentType", expression = "java(type.documentType())")
	CertificateTypeResponse toResponse(CertificateType type);
}
