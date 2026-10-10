package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeeDiscountResponse;
import com.altafjava.school.domain.fee.model.FeeDiscount;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeDiscountMapper {

	@Mapping(target = "publicId", expression = "java(discount.getPublicId().toString())")
	@Mapping(target = "discountType", expression = "java(discount.getDiscountType().name())")
	@Mapping(target = "active", expression = "java(discount.isActive())")
	@Mapping(target = "grantedAt", source = "createdAt")
	@Mapping(target = "grantedByUserPublicId", source = "grantedByUserId", qualifiedByName = "user")
	@Mapping(target = "feeStructurePublicId", source = "feeStructureId", qualifiedByName = "feeStructure")
	FeeDiscountResponse toResponse(FeeDiscount discount);
}
