package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.ExamService;
import com.altafjava.school.application.service.GradeService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.subject.model.Subject;

/**
 * Verifies that grade records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class GradeTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private GradeService gradeService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private ExamService examService;

	@Autowired
	private SubjectService subjectService;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "grd-a-" + suffix, 1L, "admin@grd-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "grd-b-" + suffix, 1L, "admin@grd-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private String createAcademicYear(String name) {
		AcademicYear academicYear = academicYearService.create(name, LocalDate.of(2024, 6, 1),
				LocalDate.of(2025, 5, 31), true);
		return academicYear.getPublicId().toString();
	}

	// SchoolTenantProvisioningListener seeds UNIT_TEST/MIDTERM/FINAL/QUIZ for every new tenant.
	private Long examTypeIdFor(String code) {
		return examTypeDefinitionRepository.findByCodeAndTenantId(code, TenantContext.getCurrentTenantId())
				.orElseThrow().getId();
	}

	@Test
	void gradeRecordedUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6), "Grade 5", "A", academicYearPublicId, null);
		Subject subject = subjectService.create("MATH-" + UUID.randomUUID().toString().substring(0, 6), "Math", null);
		Exam exam = examService.schedule("Midterm", subject.getId(), classroom.getId(),
				LocalDateTime.now().plusDays(7), BigDecimal.valueOf(100), null,
				examTypeIdFor("MIDTERM"), Exam.FULL_WEIGHTAGE);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6),
				"Alice", "Smith", "alice@a.edu", LocalDate.of(2010, 1, 1));
		gradeService.record(student.getId(), exam.getId(), BigDecimal.valueOf(85), "teacher-a");

		activateTenant(tenantB);
		Page<Grade> tenantBGrades = gradeService.listGrades(PageRequest.of(0, 100));

		boolean found = tenantBGrades.getContent().stream()
				.anyMatch(g -> tenantA.getId().equals(g.getTenantId()));
		assertFalse(found, "Tenant B must not see grades recorded under tenant A");
	}

	@Test
	void gradePublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6), "Grade 6", "B", academicYearPublicId, null);
		Subject subject = subjectService.create("SCI-" + UUID.randomUUID().toString().substring(0, 6), "Science",
				null);
		Exam exam = examService.schedule("Final", subject.getId(), classroom.getId(),
				LocalDateTime.now().plusDays(14), BigDecimal.valueOf(100), null,
				examTypeIdFor("FINAL"), Exam.FULL_WEIGHTAGE);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6),
				"Bob", "Jones", "bob@a.edu", LocalDate.of(2011, 3, 20));
		Grade grade = gradeService.record(student.getId(), exam.getId(),
				BigDecimal.valueOf(90), "teacher-a");
		String publicId = grade.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> gradeService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's grade");
	}
}
