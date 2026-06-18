package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;

@Service
public class GradeService {

	private final GradeRepository gradeRepository;

	public GradeService(GradeRepository gradeRepository) {
		this.gradeRepository = gradeRepository;
	}

	@Transactional(readOnly = true)
	public Page<Grade> listGrades(Pageable pageable) {
		return gradeRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Grade findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return gradeRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Grade not found: " + publicId));
	}

	@Transactional
	public Grade record(Long studentId, String subject, Long examId,
			BigDecimal marks, String gradeLetter, String gradedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (gradeRepository.existsByStudentIdAndExamIdAndTenantId(studentId, examId, tenantId)) {
			throw new IllegalArgumentException(
					"Grade already recorded for student " + studentId + " in exam " + examId);
		}
		Grade grade = Grade.create(studentId, subject, examId, marks, gradeLetter, gradedBy);
		return gradeRepository.save(grade);
	}
}
