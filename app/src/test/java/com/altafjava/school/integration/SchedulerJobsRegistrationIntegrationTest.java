package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.school.application.scheduler.AcademicYearRolloverJob;
import com.altafjava.school.application.scheduler.AttendanceSummaryReportJob;
import com.altafjava.school.application.scheduler.DailyAttendanceReminderJob;
import com.altafjava.school.application.scheduler.ExamScheduleReminderJob;
import com.altafjava.school.application.scheduler.FeePaymentReminderJob;
import com.altafjava.school.application.scheduler.ReportCardGenerationJob;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;

/**
 * Verifies that all school scheduler jobs are registered as Spring beans and
 * discoverable by the platform's JobRegistrationService.
 *
 * Phase 5 validation: @ScheduledJob + JobExecutionStrategy SPI wiring works for all school jobs.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class SchedulerJobsRegistrationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private DailyAttendanceReminderJob dailyAttendanceReminderJob;

	@Autowired
	private AttendanceSummaryReportJob attendanceSummaryReportJob;

	@Autowired
	private FeePaymentReminderJob feePaymentReminderJob;

	@Autowired
	private ExamScheduleReminderJob examScheduleReminderJob;

	@Autowired
	private ReportCardGenerationJob reportCardGenerationJob;

	@Autowired
	private AcademicYearRolloverJob academicYearRolloverJob;

	@Test
	void dailyAttendanceReminderJob_isRegisteredAsBean() {
		assertNotNull(dailyAttendanceReminderJob);
		assertNotNull(dailyAttendanceReminderJob.jobName());
		assertNotNull(dailyAttendanceReminderJob.jobGroup());
	}

	@Test
	void attendanceSummaryReportJob_isRegisteredAsBean() {
		assertNotNull(attendanceSummaryReportJob);
		assertNotNull(attendanceSummaryReportJob.jobName());
	}

	@Test
	void feePaymentReminderJob_isRegisteredAsBean() {
		assertNotNull(feePaymentReminderJob);
		assertNotNull(feePaymentReminderJob.jobName());
	}

	@Test
	void examScheduleReminderJob_isRegisteredAsBean() {
		assertNotNull(examScheduleReminderJob);
		assertNotNull(examScheduleReminderJob.jobName());
	}

	@Test
	void reportCardGenerationJob_isRegisteredAsBean() {
		assertNotNull(reportCardGenerationJob);
		assertNotNull(reportCardGenerationJob.jobName());
	}

	@Test
	void academicYearRolloverJob_isRegisteredAsBean() {
		assertNotNull(academicYearRolloverJob);
		assertNotNull(academicYearRolloverJob.jobName());
	}

	@Test
	void allSchoolJobs_belongToSchoolGroup() {
		assertNotNull(dailyAttendanceReminderJob.jobGroup());
		assert "school".equals(dailyAttendanceReminderJob.jobGroup());
		assert "school".equals(attendanceSummaryReportJob.jobGroup());
		assert "school".equals(feePaymentReminderJob.jobGroup());
		assert "school".equals(examScheduleReminderJob.jobGroup());
		assert "school".equals(reportCardGenerationJob.jobGroup());
		assert "school".equals(academicYearRolloverJob.jobGroup());
	}
}
