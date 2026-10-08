package com.altafjava.school.application.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.ExamResultVisibilityPolicy;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.grade.service.GpaCalculator;
import com.altafjava.school.domain.grade.service.GpaCalculator.WeightedPoints;
import com.altafjava.school.domain.grade.service.GradeCalculator;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

/**
 * GPA rollup — average of each recorded {@code Grade}'s resolved points weighted by its exam's
 * weightage (via the grading scale effective for that grade's classroom at calculation time, see
 * {@code GradingScaleService.resolveEffectiveThresholds} — GPA reflects the currently effective
 * scale, the same pragmatic assumption {@code GradeCalculator} already makes for letter grades).
 * Callers who may not see unpublished results get a GPA over published exams only.
 * Term/year windows are applied as a date-range filter on {@code Exam.scheduledAt}, mirroring
 * {@code ReportCardService}'s existing approach rather than relying on the nullable
 * {@code Exam.termId}.
 */
@Service
public class StudentGpaService {

	private final GradeRepository gradeRepository;
	private final ExamRepository examRepository;
	private final TermRepository termRepository;
	private final AcademicYearRepository academicYearRepository;
	private final StudentRepository studentRepository;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final GradingScaleService gradingScaleService;
	private final ExamResultVisibilityPolicy examResultVisibilityPolicy;
	private final GradeCalculator gradeCalculator = new GradeCalculator();
	private final GpaCalculator gpaCalculator = new GpaCalculator();

	public StudentGpaService(GradeRepository gradeRepository, ExamRepository examRepository,
			TermRepository termRepository, AcademicYearRepository academicYearRepository,
			StudentRepository studentRepository, StudentDataAccessGuard studentDataAccessGuard,
			GradingScaleService gradingScaleService, ExamResultVisibilityPolicy examResultVisibilityPolicy) {
		this.gradeRepository = gradeRepository;
		this.examRepository = examRepository;
		this.termRepository = termRepository;
		this.academicYearRepository = academicYearRepository;
		this.studentRepository = studentRepository;
		this.studentDataAccessGuard = studentDataAccessGuard;
		this.gradingScaleService = gradingScaleService;
		this.examResultVisibilityPolicy = examResultVisibilityPolicy;
	}

	@Transactional(readOnly = true)
	public GpaResult calculateTermGpa(String studentPublicId, String termPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(tenantId, studentPublicId);
		Term term = termRepository.findByPublicIdAndTenantId(UUID.fromString(termPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Term not found: " + termPublicId));
		return calculateGpaForRange(tenantId, student.getId(), term.getStartDate().atStartOfDay(),
				term.getEndDate().atTime(LocalTime.MAX));
	}

	@Transactional(readOnly = true)
	public GpaResult calculateAcademicYearGpa(String studentPublicId, String academicYearPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(tenantId, studentPublicId);
		AcademicYear academicYear = academicYearRepository
				.findByPublicIdAndTenantId(UUID.fromString(academicYearPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearPublicId));
		return calculateGpaForRange(tenantId, student.getId(), academicYear.getStartDate().atStartOfDay(),
				academicYear.getEndDate().atTime(LocalTime.MAX));
	}

	@Transactional(readOnly = true)
	public GpaResult calculateCumulativeGpa(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(tenantId, studentPublicId);
		return toResult(examinedGrades(tenantId, student.getId())
				.map(this::resolveWeightedPoints)
				.flatMap(Optional::stream)
				.toList());
	}

	private GpaResult calculateGpaForRange(Long tenantId, Long studentId, LocalDateTime start, LocalDateTime end) {
		return toResult(examinedGrades(tenantId, studentId)
				.filter(gradedExam -> withinRange(gradedExam.exam(), start, end))
				.map(this::resolveWeightedPoints)
				.flatMap(Optional::stream)
				.toList());
	}

	private boolean withinRange(Exam exam, LocalDateTime start, LocalDateTime end) {
		return !exam.getScheduledAt().isBefore(start) && !exam.getScheduledAt().isAfter(end);
	}

	private Stream<GradedExam> examinedGrades(Long tenantId, Long studentId) {
		List<Grade> grades = gradeRepository.findByStudentId(tenantId, studentId);
		List<Long> examIds = grades.stream().map(Grade::getExamId).distinct().toList();
		if (examIds.isEmpty()) {
			return Stream.empty();
		}
		Map<Long, Exam> examsById = examRepository.findAllByIdInAndTenantId(examIds, tenantId).stream()
				.collect(Collectors.toMap(Exam::getId, Function.identity()));
		boolean includeUnpublished = examResultVisibilityPolicy.canSeeUnpublishedResults();
		return grades.stream()
				.map(grade -> Optional.ofNullable(examsById.get(grade.getExamId()))
						.filter(exam -> includeUnpublished || exam.isResultsPublished())
						.map(exam -> new GradedExam(grade, exam)))
				.flatMap(Optional::stream);
	}

	private Optional<WeightedPoints> resolveWeightedPoints(GradedExam gradedExam) {
		List<GradingScaleThreshold> thresholds = gradingScaleService
				.resolveEffectiveThresholds(gradedExam.exam().getClassroomId());
		return gradeCalculator.resolvePoints(gradedExam.grade().getGradeLetter(), thresholds)
				.map(points -> new WeightedPoints(points, gradedExam.exam().getWeightage()));
	}

	private GpaResult toResult(List<WeightedPoints> weightedPoints) {
		return new GpaResult(gpaCalculator.calculateWeightedAverage(weightedPoints).orElse(null),
				weightedPoints.size());
	}

	private Student resolveStudent(Long tenantId, String studentPublicId) {
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return student;
	}

	private record GradedExam(Grade grade, Exam exam) {
	}
}
