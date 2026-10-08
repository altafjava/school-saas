package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.BoardService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.CurriculumService;
import com.altafjava.school.application.service.GpaResult;
import com.altafjava.school.application.service.GradeService;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.application.service.GradingScaleThresholdInput;
import com.altafjava.school.application.service.StudentGpaService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.curriculum.model.Board;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.model.GradingScale;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.subject.model.Subject;

/**
 * Verifies boards/curricula/grading-scales created under tenant A are not visible to tenant B,
 * that a classroom's assigned curriculum's grading scale is actually used when grading, and that
 * GPA rolls up correctly end to end.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class CurriculumTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private BoardService boardService;

	@Autowired
	private CurriculumService curriculumService;

	@Autowired
	private GradingScaleService gradingScaleService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private SubjectService subjectService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private GradeService gradeService;

	@Autowired
	private StudentGpaService studentGpaService;

	@Autowired
	private TermService termService;

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "curr-a-" + suffix, 1L, "admin@curr-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "curr-b-" + suffix, 1L, "admin@curr-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	// SchoolTenantProvisioningListener seeds UNIT_TEST/MIDTERM/FINAL/QUIZ for every new tenant.
	private Long examTypeIdFor(String code) {
		return examTypeDefinitionRepository.findByCodeAndTenantId(code, TenantContext.getCurrentTenantId())
				.orElseThrow().getId();
	}

	private void authenticateAsTenantAdmin() {
		AuthenticatedUser principal = fixedIdPrincipal(-1L);
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	private AuthenticatedUser fixedIdPrincipal(Long userId) {
		return new AuthenticatedUser() {
			@Override
			public Long getId() {
				return userId;
			}

			@Override
			public String getUsername() {
				return "user-" + userId;
			}

			@Override
			public Long getTenantId() {
				return null;
			}
		};
	}

	private List<GradingScaleThresholdInput> ibStyleThresholds() {
		return List.of(
				new GradingScaleThresholdInput("7", new BigDecimal("90"), new BigDecimal("7.0")),
				new GradingScaleThresholdInput("1", BigDecimal.ZERO, BigDecimal.ONE));
	}

	@Test
	void boardCreatedUnderTenantA_isNotVisibleWhenListingTenantB() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		boardService.create("IB-" + UUID.randomUUID(), "IB-" + UUID.randomUUID().toString().substring(0, 6), null);

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		Page<Board> boardsB = boardService.list(PageRequest.of(0, 100));

		assertTrue(boardsB.getContent().isEmpty(), "Tenant B must not see tenant A's boards");
	}

	@Test
	void curriculumCreatedUnderTenantA_notResolvableFromTenantB() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		Board board = boardService.create("Board-" + UUID.randomUUID(),
				"BRD-" + UUID.randomUUID().toString().substring(0, 6), null);
		Curriculum curriculum = curriculumService.create(board.getPublicId().toString(), "Curriculum A", "CUR-A", null);
		String curriculumPublicId = curriculum.getPublicId().toString();

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		assertThrows(ResourceNotFoundException.class,
				() -> curriculumService.assignGradingScale(curriculumPublicId, UUID.randomUUID().toString()),
				"Tenant B must not be able to resolve tenant A's curriculum");
	}

	@Test
	void classroomWithCurriculumScale_gradesResolveToThatScale() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		AcademicYear academicYear = academicYearService.create("2026-27-" + suffix, LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31), true);
		Board board = boardService.create("IB-" + suffix, "IB-" + suffix, null);
		Curriculum curriculum = curriculumService.create(board.getPublicId().toString(), "IB Diploma-" + suffix,
				"IBD-" + suffix, null);
		GradingScale ibScale = gradingScaleService.create("IB Scale-" + suffix, ibStyleThresholds(), false);
		curriculumService.assignGradingScale(curriculum.getPublicId().toString(), ibScale.getPublicId().toString());
		Classroom classroom = classroomService.create("CLS-" + suffix, "Grade 11", "A",
				academicYear.getPublicId().toString(), null);
		classroomService.assignCurriculum(classroom.getPublicId().toString(), curriculum.getPublicId().toString());
		Subject subject = subjectService.create("SUB-" + suffix, "Physics", null);
		Student student = studentService.enroll("STU-" + suffix, "Nora", "Kim",
				"nora-" + suffix + "@school.test", LocalDate.of(2010, 4, 4));

		Exam exam = Exam.create("Final", subject.getId(), classroom.getId(), LocalDateTime.now(),
				BigDecimal.valueOf(100), null, examTypeIdFor("FINAL"), Exam.FULL_WEIGHTAGE);
		exam = examRepository.save(exam);

		Grade grade = gradeService.record(student.getId(), exam.getId(), BigDecimal.valueOf(95), "teacher");

		assertEquals("7", grade.getGradeLetter(),
				"A classroom assigned to a curriculum with its own scale must grade against that scale");
	}

	@Test
	void classroomWithoutCurriculum_fallsBackToTenantDefaultScale() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		// Explicitly seeded here rather than relying on SchoolTenantProvisioningListener's async
		// default-scale seeding completing before this assertion runs — the same reason every
		// sibling integration test creates its own AcademicYear instead of trusting that listener.
		List<GradingScaleThresholdInput> defaultThresholds = List.of(
				new GradingScaleThresholdInput("A", new BigDecimal("90"), new BigDecimal("4.0")),
				new GradingScaleThresholdInput("F", BigDecimal.ZERO, BigDecimal.ZERO));
		gradingScaleService.create("Default-" + suffix, defaultThresholds, true);
		AcademicYear academicYear = academicYearService.create("2026-27-" + suffix, LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31), true);
		Classroom classroom = classroomService.create("CLS-" + suffix, "Grade 5", "A",
				academicYear.getPublicId().toString(), null);
		Subject subject = subjectService.create("SUB-" + suffix, "Math", null);
		Student student = studentService.enroll("STU-" + suffix, "Ravi", "Shah",
				"ravi-" + suffix + "@school.test", LocalDate.of(2012, 1, 1));

		Exam exam = Exam.create("Final", subject.getId(), classroom.getId(), LocalDateTime.now(),
				BigDecimal.valueOf(100), null, examTypeIdFor("FINAL"), Exam.FULL_WEIGHTAGE);
		exam = examRepository.save(exam);

		Grade grade = gradeService.record(student.getId(), exam.getId(), BigDecimal.valueOf(95), "teacher");

		assertEquals("A", grade.getGradeLetter(),
				"A classroom with no curriculum assigned must fall back to the tenant's default scale");
	}

	@Test
	void studentGpa_rollsUpAcrossMultipleExams() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		List<GradingScaleThresholdInput> defaultThresholds = List.of(
				new GradingScaleThresholdInput("A", new BigDecimal("90"), new BigDecimal("4.0")),
				new GradingScaleThresholdInput("B", new BigDecimal("80"), new BigDecimal("3.0")),
				new GradingScaleThresholdInput("F", BigDecimal.ZERO, BigDecimal.ZERO));
		gradingScaleService.create("Default-" + suffix, defaultThresholds, true);
		AcademicYear academicYear = academicYearService.create("2026-27-" + suffix, LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31), true);
		Classroom classroom = classroomService.create("CLS-" + suffix, "Grade 8", "A",
				academicYear.getPublicId().toString(), null);
		Subject subject = subjectService.create("SUB-" + suffix, "Science", null);
		Student student = studentService.enroll("STU-" + suffix, "Mia", "Chen",
				"mia-" + suffix + "@school.test", LocalDate.of(2011, 6, 6));

		Exam examA = examRepository.save(Exam.create("Test 1", subject.getId(), classroom.getId(),
				LocalDateTime.of(2026, 5, 1, 9, 0), BigDecimal.valueOf(100), null,
				examTypeIdFor("UNIT_TEST"), Exam.FULL_WEIGHTAGE));
		Exam examB = examRepository.save(Exam.create("Test 2", subject.getId(), classroom.getId(),
				LocalDateTime.of(2026, 5, 2, 9, 0), BigDecimal.valueOf(100), null,
				examTypeIdFor("UNIT_TEST"), Exam.FULL_WEIGHTAGE));
		gradeService.record(student.getId(), examA.getId(), BigDecimal.valueOf(95), "teacher");
		gradeService.record(student.getId(), examB.getId(), BigDecimal.valueOf(85), "teacher");

		GpaResult result = studentGpaService.calculateCumulativeGpa(student.getPublicId().toString());

		assertEquals(2, result.gradeCount());
		assertEquals(0, new BigDecimal("3.50").compareTo(result.gpa()));
	}

	// TestRedisConfig mocks Redis entirely for the "test" profile, so a cache hit itself isn't
	// observable here — this verifies eviction still prevents a stale read.
	@Test
	void boardLookup_evictedOnUpdate_reflectsNewName() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		Board created = boardService.create("Stale-" + UUID.randomUUID(),
				"STL-" + UUID.randomUUID().toString().substring(0, 6), null);
		String publicId = created.getPublicId().toString();
		boardService.findByPublicId(publicId);

		boardService.updateDetails(publicId, "Renamed", created.getCode(), null);
		Board afterUpdate = boardService.findByPublicId(publicId);

		assertEquals("Renamed", afterUpdate.getName());
	}

	// assignGradingScale evicts the cached thresholds wholesale — verifies reassignment is
	// reflected immediately, not stale.
	@Test
	void resolveEffectiveThresholds_reflectsScaleReassignment_notStale() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		Board board = boardService.create("Board-" + suffix, "BRD-" + suffix, null);
		Curriculum curriculum = curriculumService.create(board.getPublicId().toString(), "Curriculum-" + suffix,
				"CUR-" + suffix, null);
		GradingScale scaleOne = gradingScaleService.create("Scale One " + suffix, List.of(
				new GradingScaleThresholdInput("A", new BigDecimal("90"), new BigDecimal("4.0")),
				new GradingScaleThresholdInput("F", BigDecimal.ZERO, BigDecimal.ZERO)), false);
		GradingScale scaleTwo = gradingScaleService.create("Scale Two " + suffix, List.of(
				new GradingScaleThresholdInput("P", new BigDecimal("50"), new BigDecimal("1.0")),
				new GradingScaleThresholdInput("F", BigDecimal.ZERO, BigDecimal.ZERO)), false);
		curriculumService.assignGradingScale(curriculum.getPublicId().toString(), scaleOne.getPublicId().toString());
		AcademicYear academicYear = academicYearService.create("AY-" + suffix, LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31), true);
		Classroom classroom = classroomService.create("CLS-" + suffix, "Grade 5", "A",
				academicYear.getPublicId().toString(), null);
		classroomService.assignCurriculum(classroom.getPublicId().toString(), curriculum.getPublicId().toString());

		List<com.altafjava.school.domain.curriculum.model.GradingScaleThreshold> firstRead = gradingScaleService
				.resolveEffectiveThresholds(classroom.getId());
		assertEquals("A", firstRead.get(0).getLetter());

		curriculumService.assignGradingScale(curriculum.getPublicId().toString(), scaleTwo.getPublicId().toString());
		List<com.altafjava.school.domain.curriculum.model.GradingScaleThreshold> afterReassignment = gradingScaleService
				.resolveEffectiveThresholds(classroom.getId());

		assertEquals("P", afterReassignment.get(0).getLetter());
	}
}
