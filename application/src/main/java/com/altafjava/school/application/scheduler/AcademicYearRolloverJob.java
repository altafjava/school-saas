package com.altafjava.school.application.scheduler;

import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;

/**
 * Runs on April 1st at 00:00 tenant-local time (start of new academic year).
 * Creates the new academic year and marks the previous one as non-current.
 */
@Component
@ScheduledJob(name = "AcademicYearRollover", group = "school", description = "Rolls over to the new academic year on April 1st", cronExpression = "0 0 0 1 4 ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class AcademicYearRolloverJob implements JobExecutionStrategy {

	private static final Logger log = LoggerFactory.getLogger(AcademicYearRolloverJob.class);

	private final AcademicYearRepository academicYearRepository;

	public AcademicYearRolloverJob(AcademicYearRepository academicYearRepository) {
		this.academicYearRepository = academicYearRepository;
	}

	@Override
	public String jobName() {
		return "AcademicYearRollover";
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
	@Transactional
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		log.info("action=academic-year-rollover tenantId={} executionId={}", tenantId, ctx.executionId());

		int year = LocalDate.now().getYear();
		String newYearName = year + "-" + (year + 1);

		academicYearRepository.findByCurrentTrueAndTenantId(tenantId)
				.ifPresent(current -> {
					current.setCurrent(false);
					academicYearRepository.save(current);
					log.info("action=academic-year-deactivated tenantId={} year={}", tenantId, current.getName());
				});

		if (!academicYearRepository.existsByNameAndTenantId(newYearName, tenantId)) {
			AcademicYear newYear = AcademicYear.create(
					newYearName,
					LocalDate.of(year, 4, 1),
					LocalDate.of(year + 1, 3, 31),
					true);
			academicYearRepository.save(newYear);
			log.info("action=academic-year-created tenantId={} year={}", tenantId, newYearName);
		}

		return new JobExecutionResult.Success(Map.of("newAcademicYear", newYearName), null);
	}
}
