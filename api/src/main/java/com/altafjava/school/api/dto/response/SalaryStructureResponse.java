package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalaryStructureResponse(
		String publicId,
		Long employeeId,
		List<PayComponentAmountResponse> components,
		BigDecimal grossPay,
		LocalDate effectiveFrom,
		boolean active) {

	public SalaryStructureResponse {
		components = List.copyOf(components);
	}
}
