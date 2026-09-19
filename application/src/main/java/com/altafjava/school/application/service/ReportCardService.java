package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.application.branding.TenantBrandingService;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.file.service.StorageService;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.application.customfield.CustomFieldValue;
import com.altafjava.school.application.reportcard.ReportCardExtras;
import com.altafjava.school.application.reportcard.ReportCardLine;
import com.altafjava.school.application.reportcard.ReportCardPdfGenerator;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.attendance.service.AttendancePercentageCalculator;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.holiday.repository.HolidayRepository;
import com.altafjava.school.domain.holiday.service.HolidayDateRangeResolver;
import com.altafjava.school.domain.reportcard.event.ReportCardGeneratedEvent;
import com.altafjava.school.domain.reportcard.model.ReportCard;
import com.altafjava.school.domain.reportcard.model.ReportCardTemplate;
import com.altafjava.school.domain.reportcard.repository.ReportCardRepository;
import com.altafjava.school.domain.reportcard.repository.ReportCardTemplateRepository;
import com.altafjava.school.domain.reportcard.service.ReportCardRankCalculator;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReportCardService {

	private final ReportCardRepository reportCardRepository;
	private final StudentRepository studentRepository;
	private final TermRepository termRepository;
	private final GradeRepository gradeRepository;
	private final ExamRepository examRepository;
	private final SubjectRepository subjectRepository;
	private final StorageService storageService;
	private final ReportCardPdfGenerator pdfGenerator;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final EventPublisher eventPublisher;
	private final TransactionTemplate transactionTemplate;
	private final TenantRepository tenantRepository;
	private final TenantBrandingService tenantBrandingService;
	private final TenantFormattingService tenantFormattingService;
	private final AttendanceRepository attendanceRepository;
	private final HolidayRepository holidayRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final ClassroomRepository classroomRepository;
	private final ReportCardTemplateRepository reportCardTemplateRepository;
	// A read-only, no-side-effect lookup service — the same precedented shape as
	// GradeService/StudentGpaService's existing reuse of GradingScaleService (see item 1.8 of the
	// configurability audit), not a new application-service-to-application-service coupling.
	private final CustomFieldValueService customFieldValueService;
	private final AttendancePercentageCalculator attendancePercentageCalculator = new AttendancePercentageCalculator();
	private final HolidayDateRangeResolver holidayDateRangeResolver = new HolidayDateRangeResolver();
	private final ReportCardRankCalculator reportCardRankCalculator = new ReportCardRankCalculator();

	public ReportCardService(ReportCardRepository reportCardRepository, StudentRepository studentRepository,
			TermRepository termRepository, GradeRepository gradeRepository, ExamRepository examRepository,
			SubjectRepository subjectRepository, StorageService storageService, ReportCardPdfGenerator pdfGenerator,
			StudentDataAccessGuard studentDataAccessGuard, EventPublisher eventPublisher,
			PlatformTransactionManager transactionManager, TenantRepository tenantRepository,
			TenantBrandingService tenantBrandingService, TenantFormattingService tenantFormattingService,
			AttendanceRepository attendanceRepository, HolidayRepository holidayRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository, ClassroomRepository classroomRepository,
			ReportCardTemplateRepository reportCardTemplateRepository,
			CustomFieldValueService customFieldValueService) {
		this.reportCardRepository = reportCardRepository;
		this.studentRepository = studentRepository;
		this.termRepository = termRepository;
		this.gradeRepository = gradeRepository;
		this.examRepository = examRepository;
		this.subjectRepository = subjectRepository;
		this.storageService = storageService;
		this.pdfGenerator = pdfGenerator;
		this.studentDataAccessGuard = studentDataAccessGuard;
		this.eventPublisher = eventPublisher;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.tenantRepository = tenantRepository;
		this.tenantBrandingService = tenantBrandingService;
		this.tenantFormattingService = tenantFormattingService;
		this.attendanceRepository = attendanceRepository;
		this.holidayRepository = holidayRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.classroomRepository = classroomRepository;
		this.reportCardTemplateRepository = reportCardTemplateRepository;
		this.customFieldValueService = customFieldValueService;
	}

	@Transactional(readOnly = true)
	public Page<ReportCard> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return reportCardRepository.findByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public ReportCard findByPublicId(String studentPublicId, String reportCardPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return reportCardRepository.findByPublicIdAndTenantId(UUID.fromString(reportCardPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Report card not found: " + reportCardPublicId));
	}

	public byte[] downloadPdf(ReportCard reportCard) {
		return storageService.downloadFile(reportCard.getStorageKey());
	}

	/**
	 * Generates (or regenerates) a student's report card for a term: aggregates every Grade
	 * whose Exam falls within the term's date range, renders a PDF, and stores it. There is no
	 * {@code termId} column on {@code Exam} — the term boundary is applied as a date-range filter
	 * on {@code Exam.scheduledAt} against {@code Term.startDate}/{@code endDate} instead, which
	 * avoids a schema change to an entity two earlier phases already shipped and tested.
	 *
	 * <p>
	 * {@code teacherRemarks}/{@code principalRemarks} are baked into the PDF at generation time
	 * (and stored on the new {@code ReportCard} row) rather than added afterward — the entity is
	 * replaced wholesale on every regeneration (see {@link #persistReportCard}), so remarks added
	 * "later" would have nowhere durable to live between one generation and the next.
	 *
	 * <p>
	 * Deliberately NOT {@code @Transactional} at this level. Building the PDF is pure computation
	 * and {@code storageService.uploadFile} is a network call to S3 — neither should hold a DB
	 * transaction (and its connection) open for their duration. The PDF is built and uploaded
	 * first; only the DB write (soft-deleting any prior report card for this term and inserting
	 * the new row) runs inside its own short transaction, via {@link #persistReportCard}. If the
	 * upload fails, execution never reaches the DB write, so no dangling row is possible. If the
	 * DB write fails after a successful upload, the now-orphaned S3 object is best-effort deleted
	 * (logged, not swallowed) before the original exception is rethrown — a failed generation
	 * should surface as a failure to the caller, not silently return a report card that was never
	 * persisted.
	 */
	public ReportCard generate(Long studentId, Long termId, String teacherRemarks, String principalRemarks) {
		return generate(studentId, termId, teacherRemarks, principalRemarks, null);
	}

	/**
	 * Same as {@link #generate(Long, Long, String, String)}, but lets a caller generating for many
	 * students in one run (see {@code ReportCardGenerationJob}) share one classroom-rank cache
	 * across all of them — the expensive part of ranking (loading every classmate's grades and
	 * computing percentages) then runs once per classroom instead of once per student.
	 */
	public ReportCard generate(Long studentId, Long termId, String teacherRemarks, String principalRemarks,
			Map<Long, Integer> classroomRankCache) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
		Term term = termRepository.findByIdAndTenantId(termId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Term not found: " + termId));

		List<ReportCardLine> lines = buildReportLines(tenantId, studentId, term);
		String tenantName = tenantRepository.findById(tenantId)
				.map(Tenant::getName)
				.orElse("");
		byte[] logoBytes = tenantBrandingService.getLogoBytes(tenantId).orElse(null);
		ReportCardExtras extras = buildExtras(tenantId, student, term, teacherRemarks, principalRemarks,
				classroomRankCache);

		byte[] pdf = pdfGenerator.generate(student, term, lines, tenantName, logoBytes,
				tenantFormattingService.resolveLocale(tenantId), extras);
		String storageKey = String.format("tenants/%d/report-cards/%d/%d/%s.pdf", tenantId, studentId, termId,
				UUID.randomUUID());
		storageService.uploadFile(storageKey, pdf, "application/pdf");

		try {
			return persistReportCard(tenantId, studentId, termId, storageKey, teacherRemarks, principalRemarks);
		} catch (RuntimeException ex) {
			log.error(
					"action=report-card-persist-failed tenantId={} studentId={} termId={} storageKey={} — cleaning up orphaned upload",
					tenantId, studentId, termId, storageKey, ex);
			try {
				storageService.deleteFile(storageKey);
			} catch (RuntimeException cleanupEx) {
				log.error("action=report-card-orphan-cleanup-failed tenantId={} storageKey={}", tenantId, storageKey,
						cleanupEx);
			}
			throw ex;
		}
	}

	// The S3 object of any report card this regeneration replaces is deleted here, after the DB
	// transaction commits — never inside it, for the same reason the new upload happens before
	// the transaction rather than inside it (see this class's Javadoc). Left uncleaned, every
	// regeneration for the same student+term would leak its predecessor's PDF in storage forever.
	private ReportCard persistReportCard(Long tenantId, Long studentId, Long termId, String storageKey,
			String teacherRemarks, String principalRemarks) {
		String[] previousStorageKey = new String[1];
		ReportCard saved = transactionTemplate.execute(status -> {
			reportCardRepository.findByStudentIdAndTermIdAndTenantId(studentId, termId, tenantId)
					.ifPresent(existing -> {
						previousStorageKey[0] = existing.getStorageKey();
						existing.softDelete("report-card-regeneration");
						reportCardRepository.save(existing);
					});

			ReportCard reportCard = ReportCard.create(studentId, termId, storageKey);
			reportCard.addRemarks(teacherRemarks, principalRemarks);
			ReportCard result = reportCardRepository.save(reportCard);
			eventPublisher.publish(new ReportCardGeneratedEvent(tenantId, studentId, termId, result.getId()));
			return result;
		});

		if (previousStorageKey[0] != null) {
			try {
				storageService.deleteFile(previousStorageKey[0]);
			} catch (RuntimeException ex) {
				log.error(
						"action=report-card-previous-version-cleanup-failed tenantId={} studentId={} termId={} storageKey={}",
						tenantId, studentId, termId, previousStorageKey[0], ex);
			}
		}
		return saved;
	}

	/**
	 * Resolves every optional section's data up front, regardless of whether {@code template}
	 * actually shows that section — cheap enough (a handful of already-indexed lookups) to compute
	 * unconditionally rather than threading the template's flags through every helper here too.
	 */
	private ReportCardExtras buildExtras(Long tenantId, Student student, Term term, String teacherRemarks,
			String principalRemarks, Map<Long, Integer> classroomRankCache) {
		ReportCardTemplate template = reportCardTemplateRepository.findByTenantId(tenantId)
				.orElseGet(ReportCardTemplate::createDefault);
		Optional<Classroom> classroom = resolveCurrentClassroom(tenantId, student.getId());
		AttendancePercentage attendancePercentage = calculateAttendancePercentage(tenantId, student.getId(), term);
		Integer rank = classroom.map(c -> computeRank(tenantId, c.getId(), term, student.getId(), classroomRankCache))
				.orElse(null);
		List<CustomFieldValue> competencyValues = customFieldValueService
				.getAllValues(CustomFieldEntityType.STUDENT, student.getId());
		return new ReportCardExtras(template.isShowAttendanceSummary(), template.isShowRemarks(),
				template.isShowCompetencyGrid(), template.isShowRank(), attendancePercentage, rank, competencyValues,
				classroom.map(Classroom::getGrade).orElse(null), classroom.map(Classroom::getSection).orElse(null),
				teacherRemarks, principalRemarks);
	}

	private Optional<Classroom> resolveCurrentClassroom(Long tenantId, Long studentId) {
		return studentClassroomLinkRepository.findByStudentId(tenantId, studentId).stream()
				.max((a, b) -> a.getEnrolledAt().compareTo(b.getEnrolledAt()))
				.flatMap(link -> classroomRepository.findByIdAndTenantId(link.getClassroomId(), tenantId));
	}

	// Same holiday-aware present/total-marked-days computation as AttendanceService#calculatePercentage,
	// independently reusing the domain calculator/resolver rather than calling that application
	// service directly (see CLAUDE.md: no application-service-to-application-service coupling).
	private AttendancePercentage calculateAttendancePercentage(Long tenantId, Long studentId, Term term) {
		LocalDate from = term.getStartDate();
		LocalDate to = term.getEndDate();
		Set<LocalDate> holidayDates = holidayDateRangeResolver
				.resolve(holidayRepository.findAllByTenantId(tenantId), from, to);
		long totalMarkedDays = holidayDates.isEmpty()
				? attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetween(studentId, tenantId, from,
						to)
				: attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenExcludingDates(studentId,
						tenantId, from, to, holidayDates);
		long presentDays = holidayDates.isEmpty()
				? attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(studentId,
						tenantId, from, to, AttendanceStatus.PRESENT)
				: attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatusExcludingDates(
						studentId, tenantId, from, to, AttendanceStatus.PRESENT, holidayDates);
		return attendancePercentageCalculator.calculate(presentDays, totalMarkedDays);
	}

	/**
	 * This student's rank among every classmate currently in {@code classroomId}, by the same
	 * term-scoped total-marks-over-total-max percentage {@link #buildReportLines} computes for the
	 * report card itself — batched across the whole classroom (one grades query, one exam-batch
	 * lookup) rather than one query per classmate.
	 *
	 * <p>
	 * When {@code classroomRankCache} is non-null (see {@code ReportCardGenerationJob}, which
	 * shares one cache across every student it generates for in a run), the first student in a
	 * given classroom populates every classmate's rank into it, so every subsequent classmate is
	 * an O(1) map lookup instead of repeating this whole classroom's worth of queries again — the
	 * difference between O(n) and O(n²) DB work across a classroom of n students.
	 */
	private Integer computeRank(Long tenantId, Long classroomId, Term term, Long studentId,
			Map<Long, Integer> classroomRankCache) {
		if (classroomRankCache != null && classroomRankCache.containsKey(studentId)) {
			return classroomRankCache.get(studentId);
		}

		List<Long> classmateIds = studentClassroomLinkRepository.findAllByClassroomId(tenantId, classroomId).stream()
				.map(StudentClassroomLink::getStudentId)
				.distinct()
				.toList();
		if (!classmateIds.contains(studentId)) {
			return null;
		}
		List<Grade> allGrades = gradeRepository.findByStudentIdInAndTenantId(classmateIds, tenantId);
		Map<Long, Exam> examsById = loadExamsById(tenantId, allGrades);
		LocalDateTime termStart = term.getStartDate().atStartOfDay();
		LocalDateTime termEnd = term.getEndDate().atTime(LocalTime.MAX);
		Map<Long, List<Grade>> gradesByStudentId = allGrades.stream()
				.filter(grade -> isWithinTerm(examsById.get(grade.getExamId()), termStart, termEnd))
				.collect(Collectors.groupingBy(Grade::getStudentId));

		Map<Long, BigDecimal> percentageByStudentId = new HashMap<>();
		for (Long classmateId : classmateIds) {
			List<Grade> grades = gradesByStudentId.getOrDefault(classmateId, List.of());
			BigDecimal totalMarks = grades.stream().map(Grade::getMarks).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal totalMax = grades.stream()
					.map(grade -> examsById.get(grade.getExamId()).getMaxMarks())
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalMax.compareTo(BigDecimal.ZERO) > 0) {
				percentageByStudentId.put(classmateId,
						totalMarks.multiply(BigDecimal.valueOf(100)).divide(totalMax, 4, RoundingMode.HALF_UP));
			}
		}

		if (classroomRankCache != null) {
			for (Long classmateId : percentageByStudentId.keySet()) {
				classroomRankCache.put(classmateId,
						reportCardRankCalculator.rankOf(classmateId, percentageByStudentId));
			}
			return classroomRankCache.get(studentId);
		}
		return percentageByStudentId.containsKey(studentId)
				? reportCardRankCalculator.rankOf(studentId, percentageByStudentId)
				: null;
	}

	/**
	 * Batches exam and subject lookups instead of issuing one query per Grade row: collect every
	 * distinct examId/subjectId up front, fetch each set with a single {@code IN} query, then
	 * assemble lines from the resulting maps — zero extra queries per row. Grades whose exam falls
	 * outside the term range are dropped before the subject batch is even loaded, preserving the
	 * original short-circuit (no subject lookup wasted on a line that won't be included).
	 */
	private List<ReportCardLine> buildReportLines(Long tenantId, Long studentId, Term term) {
		LocalDateTime termStart = term.getStartDate().atStartOfDay();
		LocalDateTime termEnd = term.getEndDate().atTime(LocalTime.MAX);

		List<Grade> grades = gradeRepository.findByStudentId(tenantId, studentId);
		Map<Long, Exam> examsById = loadExamsById(tenantId, grades);

		List<Grade> gradesInTerm = grades.stream()
				.filter(grade -> isWithinTerm(examsById.get(grade.getExamId()), termStart, termEnd))
				.toList();
		Map<Long, Subject> subjectsById = loadSubjectsById(tenantId, gradesInTerm);

		return gradesInTerm.stream()
				.map(grade -> toLine(grade, examsById.get(grade.getExamId()), subjectsById))
				.toList();
	}

	private boolean isWithinTerm(Exam exam, LocalDateTime termStart, LocalDateTime termEnd) {
		return exam != null && !exam.getScheduledAt().isBefore(termStart) && !exam.getScheduledAt().isAfter(termEnd);
	}

	private Map<Long, Exam> loadExamsById(Long tenantId, List<Grade> grades) {
		List<Long> examIds = grades.stream().map(Grade::getExamId).distinct().toList();
		if (examIds.isEmpty()) {
			return Map.of();
		}
		return examRepository.findAllByIdInAndTenantId(examIds, tenantId).stream()
				.collect(Collectors.toMap(Exam::getId, Function.identity()));
	}

	private Map<Long, Subject> loadSubjectsById(Long tenantId, List<Grade> grades) {
		List<Long> subjectIds = grades.stream().map(Grade::getSubjectId).distinct().toList();
		if (subjectIds.isEmpty()) {
			return Map.of();
		}
		return subjectRepository.findAllByIdInAndTenantId(subjectIds, tenantId).stream()
				.collect(Collectors.toMap(Subject::getId, Function.identity()));
	}

	private ReportCardLine toLine(Grade grade, Exam exam, Map<Long, Subject> subjectsById) {
		String subjectName = Optional.ofNullable(subjectsById.get(grade.getSubjectId()))
				.map(Subject::getName)
				.orElse("Unknown Subject");
		return new ReportCardLine(subjectName, exam.getTitle(), grade.getMarks(), exam.getMaxMarks(),
				grade.getGradeLetter());
	}
}
