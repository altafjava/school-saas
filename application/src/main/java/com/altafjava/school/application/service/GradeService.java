package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.ExamResultVisibilityPolicy;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.model.GradeCorrection;
import com.altafjava.school.domain.grade.repository.GradeCorrectionRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.grade.service.GradeCalculator;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class GradeService {

	private final GradeRepository gradeRepository;
	private final GradeCorrectionRepository gradeCorrectionRepository;
	private final StudentRepository studentRepository;
	private final ExamRepository examRepository;
	private final GradingScaleService gradingScaleService;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final AcademicScopeResolver academicScopeResolver;
	private final AcademicAccessGuard academicAccessGuard;
	private final ExamResultVisibilityPolicy examResultVisibilityPolicy;
	private final GradeCalculator gradeCalculator = new GradeCalculator();

	public GradeService(GradeRepository gradeRepository, GradeCorrectionRepository gradeCorrectionRepository,
			StudentRepository studentRepository, ExamRepository examRepository,
			GradingScaleService gradingScaleService, StudentDataAccessGuard studentDataAccessGuard,
			AcademicScopeResolver academicScopeResolver, AcademicAccessGuard academicAccessGuard,
			ExamResultVisibilityPolicy examResultVisibilityPolicy) {
		this.gradeRepository = gradeRepository;
		this.gradeCorrectionRepository = gradeCorrectionRepository;
		this.studentRepository = studentRepository;
		this.examRepository = examRepository;
		this.gradingScaleService = gradingScaleService;
		this.studentDataAccessGuard = studentDataAccessGuard;
		this.academicScopeResolver = academicScopeResolver;
		this.academicAccessGuard = academicAccessGuard;
		this.examResultVisibilityPolicy = examResultVisibilityPolicy;
	}

	// Narrowed to the caller's scope: every exam, the exams of subjects they teach, or their own
	// students' published results.
	@Transactional(readOnly = true)
	public Page<Grade> listGrades(Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicScope scope = academicScopeResolver.current(tenantId);
		if (scope.readsAllClassrooms()) {
			return gradeRepository.findAllByTenantId(tenantId, pageable);
		}
		List<Long> taughtExamIds = examRepository
				.findAllByClassroomIdInAndTenantId(scope.teaching().classroomIds(), tenantId).stream()
				.filter(exam -> scope.teaching().teachesSubject(exam.getClassroomId(), exam.getSubjectId()))
				.map(Exam::getId)
				.toList();
		return gradeRepository.findVisible(tenantId, taughtExamIds, scope.ownStudentIds(), pageable);
	}

	@Transactional(readOnly = true)
	public Grade findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Grade grade = requireGrade(tenantId, publicId);
		assertVisibleToCaller(tenantId, grade);
		return grade;
	}

	private Grade requireGrade(Long tenantId, String publicId) {
		return gradeRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Grade not found: " + publicId));
	}

	// Mirrors getStudentGrades: staff see every grade; anyone else must own the student's record
	// and only ever sees grades of published exams.
	private void assertVisibleToCaller(Long tenantId, Grade grade) {
		if (examResultVisibilityPolicy.canSeeUnpublishedResults()) {
			return;
		}
		Student student = studentRepository.findByIdAndTenantId(grade.getStudentId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + grade.getStudentId()));
		studentDataAccessGuard.assertCanView(tenantId, student.getPublicId().toString());
		boolean published = examRepository.findByIdAndTenantId(grade.getExamId(), tenantId)
				.map(Exam::isResultsPublished).orElse(false);
		if (!published) {
			throw new ResourceNotFoundException("Grade not found: " + grade.getPublicId());
		}
	}

	@Transactional(readOnly = true)
	public Page<Grade> getStudentGrades(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		if (examResultVisibilityPolicy.canSeeUnpublishedResults()) {
			return gradeRepository.findByStudentIdAndTenantId(student.getId(), tenantId, pageable);
		}
		return gradeRepository.findPublishedByStudentId(student.getId(), tenantId, pageable);
	}

	@Transactional
	public Grade record(String studentPublicId, String examPublicId, BigDecimal marks, String gradedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long studentId = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId))
				.getId();
		Exam exam = examRepository.findByPublicIdAndTenantId(UUID.fromString(examPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examPublicId));
		if (gradeRepository.existsByStudentIdAndExamIdAndTenantId(studentId, exam.getId(), tenantId)) {
			throw new IllegalArgumentException(
					"Grade already recorded for student " + studentPublicId + " in exam " + examPublicId);
		}
		academicAccessGuard.assertCanWriteSubject(tenantId, exam.getClassroomId(), exam.getSubjectId());
		if (exam.getStatus() == ExamStatus.CANCELLED) {
			throw new BusinessException("Grades cannot be recorded for a cancelled exam");
		}
		List<GradingScaleThreshold> thresholds = gradingScaleService.resolveEffectiveThresholds(exam.getClassroomId());
		String gradeLetter = gradeCalculator.calculateLetterGrade(marks, exam.getMaxMarks(), thresholds);
		// The subject always comes from the exam, so a grade can never disagree with its exam.
		Grade grade = Grade.create(studentId, exam.getSubjectId(), exam.getId(), marks, gradeLetter, gradedBy);
		return gradeRepository.save(grade);
	}

	/**
	 * Corrects an already-recorded grade's marks, recalculating the letter grade against the
	 * classroom's currently effective grading scale (same resolution {@link #record} uses).
	 * Records a {@link GradeCorrection} row with the pre-correction values before mutating, so the
	 * correction is reconstructable from history rather than only visible as an opaque
	 * {@code updatedAt} bump.
	 */
	@Transactional
	public Grade correct(String publicId, BigDecimal marks, ExpectedVersion expectedVersion) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Grade grade = requireGrade(tenantId, publicId);
		expectedVersion.verify(grade);
		Exam exam = examRepository.findByIdAndTenantId(grade.getExamId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + grade.getExamId()));
		academicAccessGuard.assertCanWriteSubject(tenantId, exam.getClassroomId(), exam.getSubjectId());
		List<GradingScaleThreshold> thresholds = gradingScaleService.resolveEffectiveThresholds(exam.getClassroomId());
		String newGradeLetter = gradeCalculator.calculateLetterGrade(marks, exam.getMaxMarks(), thresholds);

		gradeCorrectionRepository.save(GradeCorrection.record(grade.getId(), grade.getMarks(), grade.getGradeLetter(),
				marks, newGradeLetter));

		grade.correct(marks, newGradeLetter);
		return gradeRepository.save(grade);
	}

	@Transactional(readOnly = true)
	public Page<GradeCorrection> listCorrections(String gradePublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Grade grade = requireGrade(tenantId, gradePublicId);
		assertVisibleToCaller(tenantId, grade);
		return gradeCorrectionRepository.findByGradeIdAndTenantId(tenantId, grade.getId(), pageable);
	}
}
