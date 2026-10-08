package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeeDiscountResponse;
import com.altafjava.school.domain.fee.model.FeeDiscount;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeDiscountMapper {

	@Mapping(target = "publicId", expression = "java(discount.getPublicId().toString())")
	@Mapping(target = "discountType", expression = "java(discount.getDiscountType().name())")
	@Mapping(target = "active", expression = "java(discount.isActive())")
	@Mapping(target = "grantedAt", source = "createdAt")
	@Mapping(target = "grantedByUserId", source = "grantedByUserId")
	FeeDiscountResponse toResponse(FeeDiscount discount);
}
