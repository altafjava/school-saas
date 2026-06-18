package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;

@Service
public class ExamService {

	private final ExamRepository examRepository;

	public ExamService(ExamRepository examRepository) {
		this.examRepository = examRepository;
	}

	@Transactional(readOnly = true)
	public Page<Exam> listExams(Pageable pageable) {
		return examRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Exam findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return examRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + publicId));
	}

	@Transactional
	public Exam schedule(String title, String subject, Long classroomId,
			LocalDateTime scheduledAt, BigDecimal maxMarks) {
		Exam exam = Exam.create(title, subject, classroomId, scheduledAt, maxMarks);
		return examRepository.save(exam);
	}
}
