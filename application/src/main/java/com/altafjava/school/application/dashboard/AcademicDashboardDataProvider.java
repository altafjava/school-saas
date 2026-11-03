package com.altafjava.school.application.dashboard;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.report.provider.ReportDataProvider;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;

/**
 * Academic summary: one row per recorded grade letter (a real distribution, more useful to an
 * academic-affairs viewer than a single averaged number), plus overall grade/exam counts — all
 * computed via single GROUP BY / COUNT queries, not a per-student loop.
 */
@Component
public class AcademicDashboardDataProvider implements ReportDataProvider {

	private static final int UPCOMING_EXAM_WINDOW_DAYS = 30;

	private final GradeRepository gradeRepository;
	private final ExamRepository examRepository;
	private final SchoolClock schoolClock;

	public AcademicDashboardDataProvider(GradeRepository gradeRepository, ExamRepository examRepository,
			SchoolClock schoolClock) {
		this.gradeRepository = gradeRepository;
		this.examRepository = examRepository;
		this.schoolClock = schoolClock;
	}

	@Override
	public List<Map<String, Object>> fetchData(Map<String, Object> parameters) {
		Long tenantId = TenantContext.getCurrentTenantId();
		long totalGradesRecorded = gradeRepository.countByTenantId(tenantId);
		LocalDateTime now = schoolClock.now(tenantId);
		long upcomingExamCount = examRepository.countByTenantIdAndScheduledAtBetween(tenantId, now,
				now.plusDays(UPCOMING_EXAM_WINDOW_DAYS));

		List<Object[]> distribution = gradeRepository.countGroupedByGradeLetter(tenantId);
		// totalGradesRecorded/upcomingExamCount are embedded on every row below rather than
		// returned as a separate summary row, so a tenant with zero grades still surfaces them —
		// an empty distribution must not silently produce an empty dashboard.
		if (distribution.isEmpty()) {
			Map<String, Object> emptyRow = new LinkedHashMap<>();
			emptyRow.put("gradeLetter", null);
			emptyRow.put("count", 0L);
			emptyRow.put("totalGradesRecorded", totalGradesRecorded);
			emptyRow.put("upcomingExamCount", upcomingExamCount);
			return List.of(emptyRow);
		}

		return distribution.stream()
				.map(row -> toDistributionRow(row, totalGradesRecorded, upcomingExamCount))
				.toList();
	}

	private Map<String, Object> toDistributionRow(Object[] row, long totalGradesRecorded, long upcomingExamCount) {
		Map<String, Object> distributionRow = new LinkedHashMap<>();
		distributionRow.put("gradeLetter", row[0]);
		distributionRow.put("count", row[1]);
		distributionRow.put("totalGradesRecorded", totalGradesRecorded);
		distributionRow.put("upcomingExamCount", upcomingExamCount);
		return distributionRow;
	}
}
