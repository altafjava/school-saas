package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.lifecycle.LifecycleChange;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.lifecycle.model.LifecycleStage;
import com.altafjava.school.domain.lifecycle.model.LifecycleTransition;
import com.altafjava.school.domain.lifecycle.repository.LifecycleTransitionRepository;
import com.altafjava.school.domain.student.model.Student;

/**
 * Proves the unified timeline against the real database: an applicant's history continues
 * unbroken into the enrolled student's, status changes are recorded with who/why/when, and one
 * tenant can never read another's history.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
class StudentLifecycleIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private AdmissionService admissionService;
	@Autowired
	private StudentService studentService;
	@Autowired
	private LifecycleTransitionRepository lifecycleTransitionRepository;
	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand("Lifecycle A", "lc-a-" + suffix, 1L,
				"admin@lc-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand("Lifecycle B", "lc-b-" + suffix, 1L,
				"admin@lc-b.test", "Password123!", "USD"));
		activate(tenantA);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void activate(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private List<LifecycleStage> stagesOf(List<LifecycleTransition> timeline) {
		return timeline.stream().map(LifecycleTransition::getToStage).toList();
	}

	@Test
	void applicantsHistoryContinuesUnbrokenIntoTheStudentsTimeline() {
		String code = "STU-" + UUID.randomUUID().toString().substring(0, 8);
		Admission submitted = admissionService.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith",
				"bob-" + code + "@family.test", "+14155552671", "Grade 3");
		admissionService.markUnderReview(submitted.getPublicId().toString());
		Admission enrolled = admissionService.finalizeApproval(submitted.getPublicId().toString(), "admin", "ok",
				code);

		List<LifecycleTransition> timeline = lifecycleTransitionRepository.findTimelineForStudent(tenantA.getId(),
				enrolled.getEnrolledStudentId());

		assertEquals(List.of(LifecycleStage.SUBMITTED, LifecycleStage.UNDER_REVIEW, LifecycleStage.APPROVED,
				LifecycleStage.ENROLLED), stagesOf(timeline));
		LifecycleTransition enrolment = timeline.get(3);
		assertEquals(enrolled.getId(), enrolment.getAdmissionId(), "the enrolment row belongs to both records");
		assertEquals(enrolled.getEnrolledStudentId(), enrolment.getStudentId());
	}

	@Test
	void statusChangesAreRecordedWithTheirReasonAndEffectiveDate() {
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 8), "Bob", "Jones",
				"bob@lc.test", LocalDate.of(2011, 1, 1));
		String publicId = student.getPublicId().toString();

		studentService.suspend(publicId, LifecycleChange.of("Exam malpractice"));
		studentService.reinstate(publicId, LifecycleChange.NONE);
		studentService.withdraw(publicId, new LifecycleChange("Family relocated", LocalDate.now().minusDays(3)));

		List<LifecycleTransition> timeline = lifecycleTransitionRepository.findTimelineForStudent(tenantA.getId(),
				student.getId());
		assertEquals(List.of(LifecycleStage.ENROLLED, LifecycleStage.SUSPENDED, LifecycleStage.ENROLLED,
				LifecycleStage.WITHDRAWN), stagesOf(timeline));
		LifecycleTransition withdrawal = timeline.get(3);
		assertEquals("Family relocated", withdrawal.getReason());
		assertEquals(LocalDate.now().minusDays(3), withdrawal.getEffectiveOn());
		assertEquals(LifecycleStage.ENROLLED, withdrawal.getFromStage());
	}

	@Test
	void aWithdrawnStudentCannotWithdrawOrGraduateAgain() {
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 8), "Cy", "Lee",
				"cy@lc.test", LocalDate.of(2011, 1, 1));
		String publicId = student.getPublicId().toString();
		studentService.withdraw(publicId, LifecycleChange.NONE);

		assertThrows(BusinessException.class, () -> studentService.withdraw(publicId, LifecycleChange.NONE));
		assertThrows(BusinessException.class, () -> studentService.graduate(publicId, LifecycleChange.NONE));
		assertEquals(2, lifecycleTransitionRepository.findTimelineForStudent(tenantA.getId(), student.getId()).size(),
				"rejected changes must leave no history");
	}

	@Test
	void anotherTenantCannotSeeTheHistory() {
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 8), "Di", "Park",
				"di@lc.test", LocalDate.of(2011, 1, 1));

		assertTrue(lifecycleTransitionRepository.findTimelineForStudent(tenantB.getId(), student.getId()).isEmpty());
	}
}
