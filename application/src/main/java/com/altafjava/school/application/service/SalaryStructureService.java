package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import com.altafjava.school.domain.payroll.model.PayComponentDefinition;
import com.altafjava.school.domain.payroll.model.SalaryStructure;
import com.altafjava.school.domain.payroll.repository.PayComponentDefinitionRepository;
import com.altafjava.school.domain.payroll.repository.SalaryStructureRepository;

@Service
public class SalaryStructureService {

	private final SalaryStructureRepository salaryStructureRepository;
	private final EmployeeRepository employeeRepository;
	private final PayComponentDefinitionRepository payComponentDefinitionRepository;

	public SalaryStructureService(SalaryStructureRepository salaryStructureRepository,
			EmployeeRepository employeeRepository, PayComponentDefinitionRepository payComponentDefinitionRepository) {
		this.salaryStructureRepository = salaryStructureRepository;
		this.employeeRepository = employeeRepository;
		this.payComponentDefinitionRepository = payComponentDefinitionRepository;
	}

	@Transactional(readOnly = true)
	public Page<SalaryStructure> listForEmployee(String employeePublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = findEmployee(tenantId, employeePublicId);
		return salaryStructureRepository.findAllByEmployeeIdAndTenantId(employee.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public SalaryStructure findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return salaryStructureRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + publicId));
	}

	/**
	 * @param componentAmountsByCode
	 *                                   amount per {@code PayComponentDefinition.code}; name/type
	 *                                   are resolved from the tenant's catalog, never the caller.
	 */
	@Transactional
	public SalaryStructure create(String employeePublicId, Map<String, BigDecimal> componentAmountsByCode,
			LocalDate effectiveFrom) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = findEmployee(tenantId, employeePublicId);
		return supersedeForEmployee(tenantId, employee.getId(), componentAmountsByCode, effectiveFrom);
	}

	@Transactional
	public SalaryStructure supersede(String currentStructurePublicId, Map<String, BigDecimal> componentAmountsByCode,
			LocalDate effectiveFrom) {
		Long tenantId = TenantContext.getCurrentTenantId();
		SalaryStructure current = salaryStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(currentStructurePublicId), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Salary structure not found: " + currentStructurePublicId));
		return supersedeForEmployee(tenantId, current.getEmployeeId(), componentAmountsByCode, effectiveFrom);
	}

	/**
	 * At most one salary structure may be active per employee. Deactivates any existing active
	 * structure before saving the new one, mirroring how {@code AcademicYearService} flips the
	 * previous {@code current} academic year.
	 */
	private SalaryStructure supersedeForEmployee(Long tenantId, Long employeeId,
			Map<String, BigDecimal> componentAmountsByCode, LocalDate effectiveFrom) {
		List<PayComponentAmount> components = resolveComponents(tenantId, componentAmountsByCode);
		salaryStructureRepository.findByEmployeeIdAndActiveTrueAndTenantId(employeeId, tenantId)
				.ifPresent(existing -> {
					existing.deactivate();
					salaryStructureRepository.save(existing);
				});
		SalaryStructure structure = SalaryStructure.create(employeeId, components, effectiveFrom);
		return salaryStructureRepository.save(structure);
	}

	// Each amount must reference a pay component the tenant has actually defined and kept active —
	// catches typos and stale codes from a component that was since renamed/deactivated, rather
	// than persisting an orphaned or spoofable code/name/type.
	private List<PayComponentAmount> resolveComponents(Long tenantId, Map<String, BigDecimal> componentAmountsByCode) {
		return componentAmountsByCode.entrySet().stream()
				.map(entry -> {
					PayComponentDefinition definition = payComponentDefinitionRepository
							.findByCodeAndTenantId(entry.getKey(), tenantId)
							.orElseThrow(() -> new BusinessException("Unknown pay component: " + entry.getKey()));
					if (!definition.isActive()) {
						throw new BusinessException("Pay component is not active: " + entry.getKey());
					}
					return new PayComponentAmount(definition.getCode(), definition.getName(), definition.getType(),
							entry.getValue());
				})
				.toList();
	}

	private Employee findEmployee(Long tenantId, String employeePublicId) {
		return employeeRepository.findByPublicIdAndTenantId(UUID.fromString(employeePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeePublicId));
	}
}
