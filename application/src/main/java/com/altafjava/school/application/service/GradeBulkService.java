package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.bulk.BulkEntryResult;
import com.altafjava.school.application.bulk.BulkEntryResult.Outcome;
import com.altafjava.school.application.bulk.BulkEntryResult.Row;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.domain.curriculum.model.GradeBand;
import com.altafjava.school.domain.error.AcademicErrorCode;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.model.GradeCorrection;
import com.altafjava.school.domain.grade.repository.GradeCorrectionRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.grade.service.GradeCalculator;
import com.altafjava.school.domain.student.model.Student;
import lombok.RequiredArgsConstructor;

/** Records an exam's marks in one request. Sending the same sheet twice changes nothing. */
@Service
@RequiredArgsConstructor
public class GradeBulkService {

	public record Entry(String studentPublicId, BigDecimal marks) {
	}

	private record Sheet(Exam exam, List<GradeBand> bands, String gradedBy, Map<Long, Grade> recorded) {
	}

	private final GradeCalculator gradeCalculator = new GradeCalculator();
	private final GradeRepository gradeRepository;
	private final GradeCorrectionRepository gradeCorrectionRepository;
	private final ExamRepository examRepository;
	private final GradingScaleService gradingScaleService;
	private final ClassRoster classRoster;
	private final AcademicAccessGuard academicAccessGuard;

	@Transactional
	public BulkEntryResult record(String examPublicId, List<Entry> entries, String gradedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Exam exam = examRepository.findByPublicIdAndTenantId(UUID.fromString(examPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examPublicId));
		academicAccessGuard.assertCanWriteSubject(tenantId, exam.getClassroomId(), exam.getSubjectId());
		if (exam.getStatus() == ExamStatus.CANCELLED) {
			throw new BusinessException(AcademicErrorCode.EXAM_CANCELLED,
					"Grades cannot be recorded for a cancelled exam");
		}
		exam.requireMarksOpen();

		Map<String, Student> roster = classRoster.byPublicId(tenantId, exam.getClassroomId());
		Sheet sheet = new Sheet(exam, gradingScaleService.resolveEffectiveThresholds(exam.getClassroomId()), gradedBy,
				gradeRepository.findAllByExamIdAndTenantId(exam.getId(), tenantId).stream()
						.collect(Collectors.toMap(Grade::getStudentId, Function.identity())));
		List<Row> rows = new ArrayList<>();
		for (Entry entry : entries) {
			rows.add(apply(sheet, roster.get(entry.studentPublicId()), entry));
		}
		return BulkEntryResult.of(rows);
	}

	private Row apply(Sheet sheet, Student student, Entry entry) {
		if (student == null) {
			return Row.failed(entry.studentPublicId(), "Student is not enrolled in this exam's classroom");
		}
		if (entry.marks().compareTo(sheet.exam().getMaxMarks()) > 0) {
			return Row.failed(entry.studentPublicId(), "Marks exceed the exam's maximum");
		}
		String letter = gradeCalculator.calculateLetterGrade(entry.marks(), sheet.exam().getMaxMarks(), sheet.bands());
		Grade existing = sheet.recorded().get(student.getId());
		return existing == null ? create(sheet, student, entry, letter) : reconcile(existing, entry, letter);
	}

	private Row create(Sheet sheet, Student student, Entry entry, String letter) {
		Exam exam = sheet.exam();
		Grade saved = gradeRepository.save(Grade.create(student.getId(), exam.getSubjectId(), exam.getId(),
				entry.marks(), letter, sheet.gradedBy()));
		sheet.recorded().put(student.getId(), saved);
		return Row.written(entry.studentPublicId(), Outcome.CREATED, saved.getPublicId().toString());
	}

	private Row reconcile(Grade existing, Entry entry, String letter) {
		String recordPublicId = existing.getPublicId().toString();
		if (existing.getMarks().compareTo(entry.marks()) == 0) {
			return Row.written(entry.studentPublicId(), Outcome.UNCHANGED, recordPublicId);
		}
		gradeCorrectionRepository.save(GradeCorrection.record(existing.getId(), existing.getMarks(),
				existing.getGradeLetter(), entry.marks(), letter));
		existing.correct(entry.marks(), letter);
		gradeRepository.save(existing);
		return Row.written(entry.studentPublicId(), Outcome.UPDATED, recordPublicId);
	}
}
