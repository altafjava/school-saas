package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class TeacherService {

	private final TeacherRepository teacherRepository;

	public TeacherService(TeacherRepository teacherRepository) {
		this.teacherRepository = teacherRepository;
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
		if (teacherRepository.existsByEmployeeCodeAndTenantId(employeeCode, tenantId)) {
			throw new IllegalArgumentException("Employee code already exists: " + employeeCode);
		}
		Teacher teacher = Teacher.create(employeeCode, firstName, lastName, email, joinDate);
		return teacherRepository.save(teacher);
	}
}
