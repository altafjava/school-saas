package com.altafjava.school.application.filter;

import com.altafjava.school.domain.payroll.model.PayslipStatus;

/** Narrows the payslip list; every part is optional. */
public record PayslipFilter(String employeePublicId, Integer payYear, Integer payMonth, PayslipStatus status) {

	public static final PayslipFilter NONE = new PayslipFilter(null, null, null, null);
}
