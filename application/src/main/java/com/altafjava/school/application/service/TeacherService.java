package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.employee.EmployeeCodeAllocator;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;

/**
 * Teaching staff: listing and hiring. Everything else about a teacher — contact details, HR
 * details, probation, leaving — is an employee operation (see {@link EmployeeService}); a teacher's
 * public id is its employee public id.
 */
@Service
@RequiredArgsConstructor
public class TeacherService {

	private final TeacherRepository teacherRepository;
	private final EmployeeCodeAllocator employeeCodeAllocator;

	/** Free-text {@code q} (blank = no filter) over the entity's identifying fields. */
	@Transactional(readOnly = true)
	public Page<Teacher> searchTeachers(Pageable pageable, String q) {
		return teacherRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
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
	@Audited(action = AuditAction.CREATE, resourceType = "Employee", details = "Teacher hired")
	public Teacher hire(String employeeCode, String firstName, String lastName, String email, LocalDate joinDate) {
		String code = employeeCodeAllocator.allocate(TenantContext.getCurrentTenantId(), employeeCode);
		return teacherRepository.save(Teacher.create(code, firstName, lastName, email, joinDate));
	}
}
