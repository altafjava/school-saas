package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeeStructureResponse;
import com.altafjava.school.domain.fee.model.FeeStructure;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeStructureMapper {

	@Mapping(target = "publicId", expression = "java(feeStructure.getPublicId().toString())")
	@Mapping(target = "frequency", expression = "java(feeStructure.getFrequency().name())")
	FeeStructureResponse toResponse(FeeStructure feeStructure);
}
