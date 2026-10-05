package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.school.domain.common.model.Address;
import com.altafjava.school.domain.common.service.PhoneNumberValidator;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.teacher.model.EmploymentType;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class TeacherService {

	private static final String EMPLOYEE_CODE_SEQUENCE = "EMPLOYEE_CODE";

	private final TeacherRepository teacherRepository;
	private final DepartmentRepository departmentRepository;
	private final NumberSequenceService numberSequenceService;
	private final PhoneNumberValidator phoneNumberValidator = new PhoneNumberValidator();

	public TeacherService(TeacherRepository teacherRepository, DepartmentRepository departmentRepository,
			NumberSequenceService numberSequenceService) {
		this.teacherRepository = teacherRepository;
		this.departmentRepository = departmentRepository;
		this.numberSequenceService = numberSequenceService;
	}

	@Transactional(readOnly = true)
	public Page<Teacher> listTeachers(Pageable pageable) {
		return teacherRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Teacher findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return teacherRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + publicId));
	}

	@Transactional
	public Teacher hire(String employeeCode, String firstName, String lastName,
			String email, LocalDate joinDate) {
		Long tenantId = TenantContext.getCurrentTenantId();
		String resolvedCode = resolveEmployeeCode(tenantId, employeeCode);
		if (teacherRepository.existsByEmployeeCodeAndTenantId(resolvedCode, tenantId)) {
			throw new IllegalArgumentException("Employee code already exists: " + resolvedCode);
		}
		Teacher teacher = Teacher.create(resolvedCode, firstName, lastName, email, joinDate);
		return teacherRepository.save(teacher);
	}

	// A caller-supplied employeeCode is an explicit override; omitting it defers to the tenant's
	// configured numbering sequence (prefix/width/reset period), defaulting to "EMP-0001" style.
	private String resolveEmployeeCode(Long tenantId, String employeeCode) {
		if (employeeCode != null && !employeeCode.isBlank()) {
			return employeeCode;
		}
		return numberSequenceService.generateNext(tenantId, EMPLOYEE_CODE_SEQUENCE, "EMP-", 4, ResetPeriod.NEVER);
	}

	@Transactional
	public Teacher updateContactDetails(String publicId, String firstName, String lastName, String email) {
		Teacher teacher = findByPublicId(publicId);
		teacher.updateContactDetails(firstName, lastName, email);
		return teacherRepository.save(teacher);
	}

	@Transactional
	public Teacher updateHrDetails(String publicId, String departmentPublicId, String qualification,
			EmploymentType employmentType) {
		Teacher teacher = findByPublicId(publicId);
		Long departmentId = resolveDepartmentId(departmentPublicId);
		teacher.assignHrDetails(departmentId, qualification, employmentType);
		return teacherRepository.save(teacher);
	}

	@Transactional
	public Teacher updatePhone(String publicId, String phone) {
		Teacher teacher = findByPublicId(publicId);
		String defaultRegion = teacher.getAddress() != null ? teacher.getAddress().getCountryCode() : null;
		if (!phoneNumberValidator.isValid(phone, defaultRegion)) {
			throw new BusinessException("Invalid phone number: " + phone);
		}
		teacher.updatePhone(phone);
		return teacherRepository.save(teacher);
	}

	@Transactional
	public Teacher updateAddress(String publicId, Address address) {
		Teacher teacher = findByPublicId(publicId);
		teacher.updateAddress(address);
		return teacherRepository.save(teacher);
	}

	@Transactional
	public Teacher setProbationPeriod(String publicId, LocalDate probationEndDate) {
		Teacher teacher = findByPublicId(publicId);
		teacher.setProbationPeriod(probationEndDate);
		return teacherRepository.save(teacher);
	}

	@Transactional
	public Teacher endProbation(String publicId) {
		Teacher teacher = findByPublicId(publicId);
		teacher.endProbation();
		return teacherRepository.save(teacher);
	}

	@Transactional
	public Teacher updatePhoto(String publicId, String filePublicId) {
		Teacher teacher = findByPublicId(publicId);
		teacher.updatePhoto(Optional.ofNullable(filePublicId).map(UUID::fromString).orElse(null));
		return teacherRepository.save(teacher);
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
