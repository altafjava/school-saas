package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.employee.EmployeeCodeAllocator;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.common.model.Address;
import com.altafjava.school.domain.common.service.PhoneNumberValidator;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.EmploymentType;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;

/**
 * HR operations on any employee — teachers included, since a teacher is an employee with the same
 * public id. Hiring here is for non-teaching staff; teachers are hired through {@code TeacherService}
 * so the teaching role row is created with them.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;
	private final ClassroomRepository classroomRepository;
	private final EmployeeCodeAllocator employeeCodeAllocator;
	private final PhoneNumberValidator phoneNumberValidator = new PhoneNumberValidator();

	@Transactional(readOnly = true)
	public Page<Employee> search(StaffCategory category, EmployeeStatus status, String q, Pageable pageable) {
		return employeeRepository.search(TenantContext.getCurrentTenantId(), category, status,
				LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public Employee findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return employeeRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + publicId));
	}

	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "Employee", details = "Employee hired")
	public Employee hire(StaffCategory category, String employeeCode, String firstName, String lastName,
			String email, LocalDate joinDate) {
		Long tenantId = TenantContext.getCurrentTenantId();
		String code = employeeCodeAllocator.allocate(tenantId, employeeCode);
		return employeeRepository.save(Employee.create(category, code, firstName, lastName, email, joinDate));
	}

	@Transactional
	public Employee updateContactDetails(String publicId, String firstName, String lastName, String email,
			ExpectedVersion expectedVersion) {
		Employee employee = requireActive(findByPublicId(publicId));
		expectedVersion.verify(employee);
		employee.updateContactDetails(firstName, lastName, email);
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee updateHrDetails(String publicId, String departmentPublicId, String designation,
			String qualification, EmploymentType employmentType, ExpectedVersion expectedVersion) {
		Employee employee = requireActive(findByPublicId(publicId));
		expectedVersion.verify(employee);
		employee.assignHrDetails(resolveDepartmentId(departmentPublicId), designation, qualification,
				employmentType);
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee updatePhone(String publicId, String phone, ExpectedVersion expectedVersion) {
		Employee employee = requireActive(findByPublicId(publicId));
		expectedVersion.verify(employee);
		String defaultRegion = employee.getAddress() != null ? employee.getAddress().getCountryCode() : null;
		if (!phoneNumberValidator.isValid(phone, defaultRegion)) {
			throw new BusinessException("Invalid phone number: " + phone);
		}
		employee.updatePhone(phone);
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee updateAddress(String publicId, Address address, ExpectedVersion expectedVersion) {
		Employee employee = requireActive(findByPublicId(publicId));
		expectedVersion.verify(employee);
		employee.updateAddress(address);
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee updatePhoto(String publicId, String filePublicId, ExpectedVersion expectedVersion) {
		Employee employee = requireActive(findByPublicId(publicId));
		expectedVersion.verify(employee);
		employee.updatePhoto(Optional.ofNullable(filePublicId).map(UUID::fromString).orElse(null));
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee setProbationPeriod(String publicId, LocalDate probationEndDate, ExpectedVersion expectedVersion) {
		Employee employee = requireActive(findByPublicId(publicId));
		expectedVersion.verify(employee);
		employee.setProbationPeriod(probationEndDate);
		return employeeRepository.save(employee);
	}

	@Transactional
	public Employee endProbation(String publicId) {
		Employee employee = requireActive(findByPublicId(publicId));
		employee.endProbation();
		return employeeRepository.save(employee);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Employee", details = "Employee left the school")
	public Employee exit(String publicId, EmployeeStatus status, LocalDate exitDate, String reason) {
		Employee employee = findByPublicId(publicId);
		if (classroomRepository.existsByClassTeacherIdAndTenantId(employee.getId(), employee.getTenantId())) {
			throw new BusinessException("Reassign this teacher's classes before recording their exit");
		}
		employee.exit(status, exitDate, reason);
		return employeeRepository.save(employee);
	}

	// A leaver's record is history: it stays readable but is no longer edited.
	private Employee requireActive(Employee employee) {
		if (!employee.isActive()) {
			throw new BusinessException("Employee has left the school and their record can no longer be edited: "
					+ employee.getStatus());
		}
		return employee;
	}

	private Long resolveDepartmentId(String departmentPublicId) {
		if (departmentPublicId == null) {
			return null;
		}
		Long tenantId = TenantContext.getCurrentTenantId();
		return departmentRepository.findByPublicIdAndTenantId(UUID.fromString(departmentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentPublicId))
				.getId();
	}
}
