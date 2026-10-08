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
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.subject.model.Subject;

/**
 * Verifies that exam records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class ExamTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private ExamService examService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

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
				"School A", "exam-a-" + suffix, 1L, "admin@exam-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "exam-b-" + suffix, 1L, "admin@exam-b.test", "Password123!", "USD"));
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
	void examScheduledUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6), "Grade 5", "A", academicYearPublicId, null);
		Subject subject = subjectService.create("MATH-" + UUID.randomUUID().toString().substring(0, 6), "Math", null);
		examService.schedule("Midterm", subject.getId(), classroom.getId(), LocalDateTime.now().plusDays(7),
				BigDecimal.valueOf(100), null, examTypeIdFor("MIDTERM"), Exam.FULL_WEIGHTAGE);

		activateTenant(tenantB);
		Page<Exam> tenantBExams = examService.listExams(PageRequest.of(0, 100));

		boolean found = tenantBExams.getContent().stream()
				.anyMatch(e -> tenantA.getId().equals(e.getTenantId()));
		assertFalse(found, "Tenant B must not see exams scheduled under tenant A");
	}

	@Test
	void examPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6), "Grade 6", "B", academicYearPublicId, null);
		Subject subject = subjectService.create("SCI-" + UUID.randomUUID().toString().substring(0, 6), "Science",
				null);
		Exam exam = examService.schedule("Final", subject.getId(), classroom.getId(),
				LocalDateTime.now().plusDays(14), BigDecimal.valueOf(100), null,
				examTypeIdFor("FINAL"), Exam.FULL_WEIGHTAGE);
		String publicId = exam.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> examService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's exam");
	}
}
