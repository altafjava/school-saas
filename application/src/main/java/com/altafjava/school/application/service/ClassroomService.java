package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;

@Service
public class ClassroomService {

	private final ClassroomRepository classroomRepository;

	public ClassroomService(ClassroomRepository classroomRepository) {
		this.classroomRepository = classroomRepository;
	}

	@Transactional(readOnly = true)
	public Page<Classroom> listClassrooms(Pageable pageable) {
		return classroomRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Classroom findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return classroomRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + publicId));
	}

	@Transactional
	public Classroom create(String classCode, String grade, String section,
			String academicYear, Long classTeacherId) {
		Classroom classroom = Classroom.create(classCode, grade, section, academicYear, classTeacherId);
		return classroomRepository.save(classroom);
	}
}
