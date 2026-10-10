package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.PayComponentAmountResponse;
import com.altafjava.school.api.dto.response.PayslipResponse;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import com.altafjava.school.domain.payroll.model.Payslip;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PayslipMapper {

	@Mapping(target = "publicId", expression = "java(payslip.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(payslip.getStatus().name())")
	@Mapping(target = "employeePublicId", source = "employeeId", qualifiedByName = "employee")
	PayslipResponse toResponse(Payslip payslip);

	PayComponentAmountResponse toResponse(PayComponentAmount component);
}
