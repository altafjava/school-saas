package com.altafjava.school.application.employee;

import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;

/**
 * One employee-code space for every kind of staff: a caller-supplied code is an explicit override,
 * omitting it defers to the tenant's configured numbering sequence ("EMP-0001" style by default).
 * A component, not a service, because both {@code TeacherService} and {@code EmployeeService} hire.
 */
@Component
@RequiredArgsConstructor
public class EmployeeCodeAllocator {

	private static final String EMPLOYEE_CODE_SEQUENCE = "EMPLOYEE_CODE";

	private final EmployeeRepository employeeRepository;
	private final NumberSequenceService numberSequenceService;

	public String allocate(Long tenantId, String requestedCode) {
		String code = requestedCode != null && !requestedCode.isBlank() ? requestedCode
				: numberSequenceService.generateNext(tenantId, EMPLOYEE_CODE_SEQUENCE, "EMP-", 4, ResetPeriod.NEVER);
		if (employeeRepository.existsByEmployeeCodeAndTenantId(code, tenantId)) {
			throw new BusinessException("Employee code already exists: " + code);
		}
		return code;
	}
}
