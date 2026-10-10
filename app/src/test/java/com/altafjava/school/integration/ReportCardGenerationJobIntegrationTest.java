package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.model.Page;
import com.altafjava.platform.core.model.Pageable;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.Notification;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.platform.domain.notification.repository.NotificationRepository;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.scheduler.ReportCardGenerationJob;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.reportcard.repository.ReportCardRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

/**
 * Proves the ReportCardGeneration scheduler job is genuinely wired end to end: running it against
 * a real tenant/student/term persists a real PDF (via the in-memory {@link TestStorageConfig})
 * tracked by a real ReportCard row, and notifies tenant admins with a real, persisted
 * Notification whose content reflects the real generated count.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
class ReportCardGenerationJobIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private ReportCardGenerationJob reportCardGenerationJob;

	@Autowired
	private StudentService studentService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private TermService termService;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ReportCardRepository reportCardRepository;

	@Autowired
	private TermRepository termRepository;

	private Tenant tenant;
	private String adminEmail;
	private Term currentTerm;

	@BeforeEach
	void createTenant() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		adminEmail = "admin@rc-" + suffix + ".test";
		tenant = onboardingService.registerTenant(
				new RegisterTenantCommand("Report Card School", "rc-" + suffix, 1L, adminEmail, "Password123!",
						"USD"));
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
		// A fresh tenant already gets a default "current year" academic year seeded by
		// SchoolTenantProvisioningListener — use a distinct name here to avoid colliding with it.
		AcademicYear academicYear = academicYearService.create("AY-" + suffix, LocalDate.now().minusMonths(6),
				LocalDate.now().plusMonths(6), true);
		currentTerm = termService.create("Term 1", LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
				academicYear.getPublicId().toString());
		// TermRepository.findCurrentByTenantId now reads the explicit is_current flag rather than
		// computing it from date ranges — TermService.create() deliberately does not auto-mark a
		// newly created term current (that's TermRolloverJob's job), so this test marks it directly.
		currentTerm.markCurrent();
		currentTerm = termRepository.save(currentTerm);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "ReportCardGeneration", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	@Test
	void execute_withActiveStudentsAndCurrentTerm_generatesRealReportCardsAndNotifiesAdmin() {
		var student1 = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice", "Smith",
				"alice@rc.test", LocalDate.of(2010, 1, 1));
		var student2 = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Bob", "Jones",
				"bob@rc.test", LocalDate.of(2011, 2, 2));

		JobExecutionResult result = reportCardGenerationJob.execute(context());

		assertTrue(result instanceof JobExecutionResult.Success, "Job must report success: " + result);
		var pageRequest = org.springframework.data.domain.PageRequest.of(0, 10);
		assertEquals(1,
				reportCardRepository.findByStudentIdAndTenantId(student1.getId(), tenant.getId(), pageRequest)
						.getTotalElements(),
				"A real ReportCard row must be persisted for student 1");
		assertEquals(1,
				reportCardRepository.findByStudentIdAndTenantId(student2.getId(), tenant.getId(), pageRequest)
						.getTotalElements(),
				"A real ReportCard row must be persisted for student 2");

		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new IllegalStateException("Admin user not found: " + adminEmail));
		Page<Notification> notifications = notificationRepository.findByUserId(admin.getId(), Pageable.of(0, 10));

		Notification notification = notifications.content().stream()
				.filter(n -> n.getType() == NotificationType.ANNOUNCEMENT)
				.filter(n -> "Report Cards Generated".equals(n.getTitle()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected a persisted Report Cards Generated notification"));

		assertEquals(tenant.getId(), notification.getTenantId());
		assertTrue(notification.getMessage().contains("2 of 2"),
				"Notification message must reference the real generated count: " + notification.getMessage());
	}

	@Test
	void execute_withNoActiveStudents_generatesNoReportCardsAndDoesNotNotify() {
		JobExecutionResult result = reportCardGenerationJob.execute(context());

		assertTrue(result instanceof JobExecutionResult.Success, "Job must report success: " + result);

		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new IllegalStateException("Admin user not found: " + adminEmail));
		Page<Notification> notifications = notificationRepository.findByUserId(admin.getId(), Pageable.of(0, 10));

		boolean found = notifications.content().stream()
				.anyMatch(n -> "Report Cards Generated".equals(n.getTitle()));
		assertFalse(found, "No notification must be sent when there are zero active students");
	}
}
