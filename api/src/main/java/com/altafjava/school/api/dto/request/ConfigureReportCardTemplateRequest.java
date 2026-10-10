package com.altafjava.school.api.dto.request;

import com.altafjava.platform.core.concurrency.Versioned;

public record ConfigureReportCardTemplateRequest(
		boolean showAttendanceSummary,
		boolean showRemarks,
		boolean showCompetencyGrid,
		boolean showRank,
		// Null on the first save, when no record exists yet.
		Long version) implements Versioned {
}
