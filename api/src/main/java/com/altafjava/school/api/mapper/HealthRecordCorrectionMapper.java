package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.HealthRecordCorrectionResponse;
import com.altafjava.school.domain.health.model.HealthRecordCorrection;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HealthRecordCorrectionMapper {

	@Mapping(target = "publicId", expression = "java(correction.getPublicId().toString())")
	@Mapping(target = "correctedBy", source = "createdBy")
	@Mapping(target = "correctedAt", source = "createdAt")
	HealthRecordCorrectionResponse toResponse(HealthRecordCorrection correction);
}
