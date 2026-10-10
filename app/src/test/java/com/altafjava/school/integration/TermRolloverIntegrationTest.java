package com.altafjava.school.integration;

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
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.scheduler.TermRolloverJob;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

/**
 * Real-DB coverage of {@link TermRolloverJob}: verifies the current-term flag actually flips in
 * the database, not just on an in-memory mock — the flag TermRepository.findCurrentByTenantId now
 * reads directly instead of computing from date ranges.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class TermRolloverIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private TermRolloverJob termRolloverJob;

	@Autowired
	private TermService termService;

	@Autowired
	private TermRepository termRepository;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenant;

	@BeforeEach
	void createTenant() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Term Rollover School", "term-rollover-" + suffix, 1L, "admin@term-rollover.test", "Password123!",
				"USD"));
		activate();
	}

	private void activate() {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "TermRollover", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	@Test
	void execute_flipsCurrentFlagFromOutgoingTermToIncomingTerm() {
		AcademicYear year = academicYearService.create("2025-26", LocalDate.of(2025, 6, 1),
				LocalDate.of(2026, 5, 31), true);
		Term outgoing = termService.create("Term 1", LocalDate.now().minusDays(60), LocalDate.now().minusDays(1),
				year.getPublicId().toString());
		outgoing.markCurrent();
		termRepository.save(outgoing);
		Term incoming = termService.create("Term 2", LocalDate.now(), LocalDate.now().plusDays(60),
				year.getPublicId().toString());

		termRolloverJob.execute(context());

		Term reloadedOutgoing = termRepository.findByIdAndTenantId(outgoing.getId(), tenant.getId()).orElseThrow();
		Term reloadedIncoming = termRepository.findByIdAndTenantId(incoming.getId(), tenant.getId()).orElseThrow();
		assertFalse(reloadedOutgoing.isCurrent());
		assertTrue(reloadedIncoming.isCurrent());
		assertTrue(termRepository.findCurrentByTenantId(tenant.getId())
				.map(current -> current.getId().equals(incoming.getId()))
				.orElse(false));
	}

	@Test
	void execute_withNoTermCoveringToday_leavesNoCurrentTerm() {
		AcademicYear year = academicYearService.create("2025-26", LocalDate.of(2025, 6, 1),
				LocalDate.of(2026, 5, 31), true);
		Term term = termService.create("Term 1", LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
				year.getPublicId().toString());

		termRolloverJob.execute(context());

		Term reloaded = termRepository.findByIdAndTenantId(term.getId(), tenant.getId()).orElseThrow();
		assertFalse(reloaded.isCurrent());
		assertTrue(termRepository.findCurrentByTenantId(tenant.getId()).isEmpty());
	}
}
