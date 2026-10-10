package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeePaymentResponse;
import com.altafjava.school.domain.fee.model.FeePayment;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeePaymentMapper {

	@Mapping(target = "publicId", expression = "java(feePayment.getPublicId().toString())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "feeStructurePublicId", source = "feeStructureId", qualifiedByName = "feeStructure")
	FeePaymentResponse toResponse(FeePayment feePayment);
}
