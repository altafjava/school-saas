package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.AttendanceStatusCount;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceSummaryReportJobTest {

	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private TenantAdminNotifier tenantAdminNotifier;

	private AttendanceSummaryReportJob job;

	@BeforeEach
	void setUp() {
		job = new AttendanceSummaryReportJob(attendanceRepository, tenantAdminNotifier,
				TestSchoolClocks.systemDefault());
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "AttendanceSummaryReport", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	@Test
	void execute_computesRealCountsAndNotifiesAdmins() {
		when(attendanceRepository.countByStatus(eq(1L), any(), any())).thenReturn(List.of(
				new AttendanceStatusCount(AttendanceStatus.PRESENT, 120L),
				new AttendanceStatusCount(AttendanceStatus.ABSENT, 8L),
				new AttendanceStatusCount(AttendanceStatus.LATE, 3L),
				new AttendanceStatusCount(AttendanceStatus.EXCUSED, 1L),
				new AttendanceStatusCount(AttendanceStatus.HALF_DAY, 2L)));
		when(tenantAdminNotifier.notifyAll(eq(1L), any(), any())).thenReturn(2);

		JobExecutionResult result = job.execute(context());

		org.mockito.ArgumentCaptor<String> messageCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		org.mockito.Mockito.verify(tenantAdminNotifier).notifyAll(eq(1L), any(), messageCaptor.capture());
		String message = messageCaptor.getValue();
		org.junit.jupiter.api.Assertions.assertTrue(message.contains("Present: 120"));
		org.junit.jupiter.api.Assertions.assertTrue(message.contains("Absent: 8"));
		org.junit.jupiter.api.Assertions.assertTrue(message.contains("Half day: 2"));

		assertEquals(new JobExecutionResult.Success(
				java.util.Map.of("present", 120L, "halfDay", 2L, "absent", 8L, "late", 3L, "excused", 1L,
						"notifiedCount", 2),
				null), result);
	}
}
