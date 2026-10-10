package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.event.ExamResultsPublishedEvent;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.repository.TermRepository;

@Service
public class ExamService {

	// Matches no stored exam, for a check made before the exam has been saved.
	private static final Long NO_EXAM_ID = -1L;

	private final ExamRepository examRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final TermRepository termRepository;
	private final ExamTypeDefinitionRepository examTypeDefinitionRepository;
	private final GradeRepository gradeRepository;
	private final EventPublisher eventPublisher;
	private final AcademicAccessGuard academicAccessGuard;

	public ExamService(ExamRepository examRepository, ClassroomRepository classroomRepository,
			SubjectRepository subjectRepository, TermRepository termRepository,
			ExamTypeDefinitionRepository examTypeDefinitionRepository, GradeRepository gradeRepository,
			EventPublisher eventPublisher, AcademicAccessGuard academicAccessGuard) {
		this.examRepository = examRepository;
		this.classroomRepository = classroomRepository;
		this.subjectRepository = subjectRepository;
		this.termRepository = termRepository;
		this.examTypeDefinitionRepository = examTypeDefinitionRepository;
		this.gradeRepository = gradeRepository;
		this.eventPublisher = eventPublisher;
		this.academicAccessGuard = academicAccessGuard;
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
	public Exam schedule(String title, Long subjectId, Long classroomId,
			LocalDateTime scheduledAt, BigDecimal maxMarks, Long termId, Long examTypeId, BigDecimal weightage) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!classroomRepository.existsByIdAndTenantId(classroomId, tenantId)) {
			throw new ResourceNotFoundException("Classroom not found: " + classroomId);
		}
		if (!subjectRepository.existsByIdAndTenantId(subjectId, tenantId)) {
			throw new ResourceNotFoundException("Subject not found: " + subjectId);
		}
		if (termId != null && !termRepository.existsByIdAndTenantId(termId, tenantId)) {
			throw new ResourceNotFoundException("Term not found: " + termId);
		}
		if (!examTypeDefinitionRepository.existsByIdAndTenantId(examTypeId, tenantId)) {
			throw new ResourceNotFoundException("Exam type not found: " + examTypeId);
		}
		Exam exam = Exam.create(title, subjectId, classroomId, scheduledAt, maxMarks, termId, examTypeId,
				weightage == null ? Exam.FULL_WEIGHTAGE : weightage);
		requireWithinTermWeightage(tenantId, exam);
		return examRepository.save(exam);
	}

	@Transactional
	public Exam reschedule(String publicId, LocalDateTime scheduledAt) {
		Exam exam = findByPublicId(publicId);
		exam.reschedule(scheduledAt);
		return examRepository.save(exam);
	}

	@Transactional
	public Exam assignTerm(String publicId, Long termId) {
		Exam exam = findByPublicId(publicId);
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!termRepository.existsByIdAndTenantId(termId, tenantId)) {
			throw new ResourceNotFoundException("Term not found: " + termId);
		}
		exam.assignTerm(termId);
		requireWithinTermWeightage(tenantId, exam);
		return examRepository.save(exam);
	}

	@Transactional
	public Exam reweight(String publicId, BigDecimal weightage) {
		Exam exam = findByPublicId(publicId);
		exam.reweight(weightage);
		requireWithinTermWeightage(TenantContext.getCurrentTenantId(), exam);
		return examRepository.save(exam);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Exam", details = "Exam results published")
	public Exam publishResults(String publicId, String publishedBy) {
		Exam exam = findByPublicId(publicId);
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!gradeRepository.existsByExamIdAndTenantId(exam.getId(), tenantId)) {
			throw new BusinessException("Results cannot be published before any grade is recorded");
		}
		exam.publishResults(publishedBy);
		Exam saved = examRepository.save(exam);
		eventPublisher.publish(new ExamResultsPublishedEvent(tenantId, saved.getId()));
		return saved;
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Exam", details = "Exam results withdrawn")
	public Exam withdrawResults(String publicId) {
		Exam exam = findByPublicId(publicId);
		exam.withdrawResults();
		return examRepository.save(exam);
	}

	private void requireWithinTermWeightage(Long tenantId, Exam exam) {
		if (exam.getTermId() == null) {
			return;
		}
		BigDecimal claimed = exam.getId() == null
				? examRepository.sumWeightageOfOtherExams(tenantId, exam.getClassroomId(), exam.getSubjectId(),
						exam.getTermId(), ExamStatus.CANCELLED, NO_EXAM_ID)
				: examRepository.sumWeightageOfOtherExams(tenantId, exam.getClassroomId(), exam.getSubjectId(),
						exam.getTermId(), ExamStatus.CANCELLED, exam.getId());
		BigDecimal total = claimed.add(exam.getWeightage());
		if (total.compareTo(Exam.FULL_WEIGHTAGE) > 0) {
			throw new BusinessException(
					"The exams of this subject in the term would weigh " + total.stripTrailingZeros().toPlainString()
							+ "%, above the 100% a subject's term result can carry");
		}
	}

	@Transactional
	public Exam complete(String publicId) {
		Exam exam = findByPublicId(publicId);
		academicAccessGuard.assertCanWriteSubject(TenantContext.getCurrentTenantId(), exam.getClassroomId(),
				exam.getSubjectId());
		exam.complete();
		return examRepository.save(exam);
	}

	@Transactional
	public Exam cancel(String publicId) {
		Exam exam = findByPublicId(publicId);
		exam.cancel();
		return examRepository.save(exam);
	}
}
