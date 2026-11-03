package com.altafjava.school.application.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;

@ExtendWith(MockitoExtension.class)
class AcademicDashboardDataProviderTest {

	@Mock
	private GradeRepository gradeRepository;
	@Mock
	private ExamRepository examRepository;

	private AcademicDashboardDataProvider provider;

	@BeforeEach
	void setUp() {
		provider = new AcademicDashboardDataProvider(gradeRepository, examRepository, TestSchoolClocks.systemDefault());
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void fetchData_withGrades_returnsOneRowPerGradeLetter() {
		when(gradeRepository.countByTenantId(1L)).thenReturn(50L);
		when(examRepository.countByTenantIdAndScheduledAtBetween(eq(1L), any(), any())).thenReturn(5L);
		when(gradeRepository.countGroupedByGradeLetter(1L)).thenReturn(List.of(
				new Object[] { "A", 20L },
				new Object[] { "B", 30L }));

		List<Map<String, Object>> result = provider.fetchData(Map.of());

		assertEquals(2, result.size());
		assertEquals("A", result.get(0).get("gradeLetter"));
		assertEquals(20L, result.get(0).get("count"));
		assertEquals(50L, result.get(0).get("totalGradesRecorded"));
		assertEquals(5L, result.get(0).get("upcomingExamCount"));
	}

	@Test
	void fetchData_noGradesYet_returnsSingleZeroRowNotEmptyList() {
		when(gradeRepository.countByTenantId(1L)).thenReturn(0L);
		when(examRepository.countByTenantIdAndScheduledAtBetween(eq(1L), any(), any())).thenReturn(0L);
		when(gradeRepository.countGroupedByGradeLetter(1L)).thenReturn(List.of());

		List<Map<String, Object>> result = provider.fetchData(Map.of());

		assertEquals(1, result.size());
		assertEquals(0L, result.get(0).get("totalGradesRecorded"));
	}
}
