package com.altafjava.school.api.mapper;

import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeeInstallmentResponse;
import com.altafjava.school.domain.fee.model.FeeInstallment;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeInstallmentMapper {

	@Mapping(target = "sharePercentage", expression = "java(percentage(installment))")
	FeeInstallmentResponse toResponse(FeeInstallment installment);

	default BigDecimal percentage(FeeInstallment installment) {
		return BigDecimal.valueOf(installment.getShareBasisPoints(), 2);
	}
}
