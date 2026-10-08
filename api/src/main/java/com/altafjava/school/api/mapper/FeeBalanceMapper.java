package com.altafjava.school.api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeeBalanceResponse;
import com.altafjava.school.api.dto.response.InstallmentStatusResponse;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.InstallmentStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeBalanceMapper {

	FeeBalanceResponse toResponse(FeeBalance feeBalance);

	List<FeeBalanceResponse> toResponseList(List<FeeBalance> feeBalances);

	@Mapping(target = "state", expression = "java(status.state().name())")
	InstallmentStatusResponse toResponse(InstallmentStatus status);
}
