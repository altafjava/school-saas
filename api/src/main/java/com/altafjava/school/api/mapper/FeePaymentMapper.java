package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeePaymentResponse;
import com.altafjava.school.domain.fee.model.FeePayment;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeePaymentMapper {

	@Mapping(target = "publicId", expression = "java(feePayment.getPublicId().toString())")
	FeePaymentResponse toResponse(FeePayment feePayment);
}
