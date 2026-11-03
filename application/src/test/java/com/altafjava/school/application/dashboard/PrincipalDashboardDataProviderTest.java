package com.altafjava.school.application.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.event.repository.EventRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class PrincipalDashboardDataProviderTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private EventRepository eventRepository;
	@Mock
	private AlertDispatchService alertDispatchService;

	private PrincipalDashboardDataProvider provider;

	@BeforeEach
	void setUp() {
		provider = new PrincipalDashboardDataProvider(studentRepository, attendanceRepository, eventRepository,
				alertDispatchService, TestSchoolClocks.systemDefault());
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		when(alertDispatchService.evaluate(eq(1L), any())).thenReturn(List.of());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void fetchData_returnsSingleSummaryRow() {
		when(studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L)).thenReturn(120L);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetween(eq(1L), any(), any())).thenReturn(100L);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(1L), any(), any(),
				eq(AttendanceStatus.PRESENT))).thenReturn(90L);
		when(eventRepository.countByTenantIdAndActiveTrueAndEventDateAfter(eq(1L), any(LocalDateTime.class)))
				.thenReturn(3L);

		List<Map<String, Object>> result = provider.fetchData(Map.of());

		assertEquals(1, result.size());
		Map<String, Object> row = result.get(0);
		assertEquals(120L, row.get("activeStudentCount"));
		assertEquals(0, new BigDecimal("90.00").compareTo((BigDecimal) row.get("attendancePercentageLast30Days")));
		assertEquals(3L, row.get("upcomingEventCount"));
		assertEquals(7, ((Map<?, ?>) row.get("activeAlertCounts")).size());
	}

	@Test
	void fetchData_noAttendanceMarked_avoidsDivisionByZero() {
		when(studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L)).thenReturn(0L);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetween(eq(1L), any(), any())).thenReturn(0L);
		when(eventRepository.countByTenantIdAndActiveTrueAndEventDateAfter(eq(1L), any(LocalDateTime.class)))
				.thenReturn(0L);

		List<Map<String, Object>> result = provider.fetchData(Map.of());

		Map<String, Object> row = result.get(0);
		assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) row.get("attendancePercentageLast30Days")));
	}
}
