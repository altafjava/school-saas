package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PayslipResponse(
		String publicId,
		String employeePublicId,
		int payYear,
		int payMonth,
		List<PayComponentAmountResponse> components,
		BigDecimal grossPay,
		BigDecimal lossOfPayDays,
		BigDecimal lossOfPayAmount,
		BigDecimal netPay,
		String status,
		LocalDateTime finalizedAt,
		LocalDateTime disbursedAt) {

	public PayslipResponse {
		components = List.copyOf(components);
	}
}
