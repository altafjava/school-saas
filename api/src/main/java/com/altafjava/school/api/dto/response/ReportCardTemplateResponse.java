package com.altafjava.school.api.dto.response;

public record ReportCardTemplateResponse(
		// Null until the school saves its first template; send it back as-is.
		Long version,
		boolean showAttendanceSummary,
		boolean showRemarks,
		boolean showCompetencyGrid,
		boolean showRank) {
}
