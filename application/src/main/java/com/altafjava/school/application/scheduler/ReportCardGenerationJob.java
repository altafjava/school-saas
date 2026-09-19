package com.altafjava.school.application.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.application.service.ReportCardService;
import com.altafjava.school.domain.reportcard.repository.ReportCardRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Triggered at end of term (configurable per tenant via job data map). Generates a real PDF
 * report card (via {@link ReportCardService}) for every active student against whichever Term's
 * date range currently contains today, then notifies tenant admins with the real counts.
 * Default schedule: last day of March and last day of October at 23:00.
 */
@Slf4j
@Component
@ScheduledJob(name = "ReportCardGeneration", group = "school", description = "Batch generates report cards for all students at end of term", cronExpression = "0 0 23 L 3,10 ?", tenantScoped = true, retryEnabled = true, maxRetries = 1)
public class ReportCardGenerationJob implements JobExecutionStrategy {

	// A retry after a partial failure re-runs this whole strategy from scratch for the tenant
	// (see TenantAwareJob) — generous enough to cover any realistic crash-to-retry gap for a
	// twice-a-year batch job, without risking skipping a student who legitimately needs a fresh
	// report card from an entirely separate, much-later run.
	private static final Duration SAME_RUN_WINDOW = Duration.ofHours(24);

	private final StudentRepository studentRepository;
	private final TermRepository termRepository;
	private final ReportCardService reportCardService;
	private final TenantAdminNotifier tenantAdminNotifier;
	private final ReportCardRepository reportCardRepository;

	public ReportCardGenerationJob(StudentRepository studentRepository, TermRepository termRepository,
			ReportCardService reportCardService, TenantAdminNotifier tenantAdminNotifier,
			ReportCardRepository reportCardRepository) {
		this.studentRepository = studentRepository;
		this.termRepository = termRepository;
		this.reportCardService = reportCardService;
		this.tenantAdminNotifier = tenantAdminNotifier;
		this.reportCardRepository = reportCardRepository;
	}

	@Override
	public String jobName() {
		return "ReportCardGeneration";
	}

	@Override
	public String jobGroup() {
		return "school";
	}

	@Override
	public boolean isTenantScoped() {
		return true;
	}

	@Override
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		log.info("action=report-card-generation tenantId={} executionId={}", tenantId, ctx.executionId());

		Term currentTerm = termRepository.findCurrentByTenantId(tenantId).orElse(null);
		if (currentTerm == null) {
			log.info("action=report-card-generation-skipped reason=no-current-term tenantId={}", tenantId);
			return new JobExecutionResult.Success(Map.of("generatedCount", 0, "notifiedCount", 0), null);
		}

		List<Student> activeStudents = studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE,
				tenantId);

		// A retry re-runs this whole method for the tenant from scratch (see TenantAwareJob) —
		// without this, a crash partway through a large roster would regenerate and re-upload
		// every already-succeeded student's PDF again, and re-fire ReportCardGeneratedEvent for
		// them a second time.
		Set<Long> alreadyGeneratedStudentIds = Set.copyOf(reportCardRepository
				.findStudentIdByTermIdAndTenantIdAndCreatedAtGreaterThanEqual(currentTerm.getId(), tenantId,
						Instant.now().minus(SAME_RUN_WINDOW)));

		// Shared across every student in this run so each classroom's ranking is computed once
		// (by whichever student in it happens to generate first), not once per student — see
		// ReportCardService#computeRank.
		Map<Long, Integer> classroomRankCache = new HashMap<>();
		int generatedCount = 0;
		for (Student student : activeStudents) {
			if (alreadyGeneratedStudentIds.contains(student.getId())) {
				generatedCount++;
				continue;
			}
			try {
				reportCardService.generate(student.getId(), currentTerm.getId(), null, null, classroomRankCache);
				generatedCount++;
			} catch (RuntimeException ex) {
				log.error("action=report-card-generation-failed studentId={} termId={}", student.getId(),
						currentTerm.getId(), ex);
			}
		}

		int notifiedCount = 0;
		if (generatedCount > 0) {
			String message = "Report cards generated for " + generatedCount + " of " + activeStudents.size()
					+ " active student(s) for term " + currentTerm.getName() + ".";
			notifiedCount = tenantAdminNotifier.notifyAll(tenantId, "Report Cards Generated", message);
		}

		log.info("action=report-card-generation-complete tenantId={} generatedCount={} notifiedCount={}", tenantId,
				generatedCount, notifiedCount);
		return new JobExecutionResult.Success(
				Map.of("generatedCount", generatedCount, "notifiedCount", notifiedCount), null);
	}
}
