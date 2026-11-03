package com.altafjava.school.application.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.AttendanceStatusCount;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceTrendDataProviderTest {

	@Mock
	private AttendanceRepository attendanceRepository;

	private AttendanceTrendDataProvider provider;

	@BeforeEach
	void setUp() {
		provider = new AttendanceTrendDataProvider(attendanceRepository, TestSchoolClocks.systemDefault());
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void fetchData_defaultPeriods_returnsEightWeeklyRows() {
		when(attendanceRepository.countByStatus(eq(1L), any(), any())).thenReturn(List.of(
				new AttendanceStatusCount(AttendanceStatus.PRESENT, 8L),
				new AttendanceStatusCount(AttendanceStatus.ABSENT, 2L)));

		List<Map<String, Object>> rows = provider.fetchData(Map.of());

		assertEquals(8, rows.size());
		assertEquals(0, new BigDecimal("80.00").compareTo((BigDecimal) rows.get(0).get("attendancePercentage")));
	}

	@Test
	void fetchData_explicitPeriods_returnsThatManyRows() {
		when(attendanceRepository.countByStatus(eq(1L), any(), any())).thenReturn(List.of());

		List<Map<String, Object>> rows = provider.fetchData(Map.of("periods", 3));

		assertEquals(3, rows.size());
	}

	@Test
	void fetchData_noAttendanceMarkedInAWeek_avoidsDivisionByZero() {
		when(attendanceRepository.countByStatus(eq(1L), any(), any())).thenReturn(List.of());

		List<Map<String, Object>> rows = provider.fetchData(Map.of("periods", 1));

		assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) rows.get(0).get("attendancePercentage")));
	}
}
