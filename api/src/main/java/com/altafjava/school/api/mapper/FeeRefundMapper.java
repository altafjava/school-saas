package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeeRefundResponse;
import com.altafjava.school.domain.fee.model.FeeRefund;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeRefundMapper {

	@Mapping(target = "publicId", expression = "java(refund.getPublicId().toString())")
	@Mapping(target = "method", expression = "java(refund.getMethod().name())")
	@Mapping(target = "status", expression = "java(refund.getStatus().name())")
	@Mapping(target = "refundedAt", source = "createdAt")
	FeeRefundResponse toResponse(FeeRefund refund);
}
