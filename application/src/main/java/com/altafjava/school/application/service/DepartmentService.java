package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;

@Service
public class DepartmentService {

	private final DepartmentRepository departmentRepository;
	private final EmployeeRepository employeeRepository;

	public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
		this.departmentRepository = departmentRepository;
		this.employeeRepository = employeeRepository;
	}

	@Transactional(readOnly = true)
	public Page<Department> list(Pageable pageable) {
		return departmentRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Department findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return departmentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found: " + publicId));
	}

	@Transactional
	public Department create(String name, String code, String description) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (departmentRepository.existsByCodeAndTenantId(code, tenantId)) {
			throw new BusinessException("Department code already exists: " + code);
		}
		return departmentRepository.save(Department.create(name, code, description));
	}

	@Transactional
	public Department updateDetails(String publicId, String name, String code, String description,
			ExpectedVersion expectedVersion) {
		Department department = findByPublicId(publicId);
		expectedVersion.verify(department);
		department.updateDetails(name, code, description);
		return departmentRepository.save(department);
	}

	@Transactional
	public Department assignHeadEmployee(String publicId, String headEmployeePublicId,
			ExpectedVersion expectedVersion) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Department department = findByPublicId(publicId);
		expectedVersion.verify(department);
		var headEmployee = employeeRepository.findByPublicIdAndTenantId(UUID.fromString(headEmployeePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + headEmployeePublicId));
		department.assignHeadEmployee(headEmployee.getId());
		return departmentRepository.save(department);
	}

	@Transactional
	public Department deactivate(String publicId) {
		Department department = findByPublicId(publicId);
		department.deactivate();
		return departmentRepository.save(department);
	}
}
