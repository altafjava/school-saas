package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.altafjava.school.application.reference.ReferenceSummary;

public record SalaryStructureResponse(
		String publicId,
		Long version,
		String employeePublicId,
		ReferenceSummary employee,
		List<PayComponentAmountResponse> components,
		BigDecimal grossPay,
		LocalDate effectiveFrom,
		boolean active) {

	public SalaryStructureResponse {
		components = List.copyOf(components);
	}
}
