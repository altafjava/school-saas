package com.altafjava.school.application.filter;

import com.altafjava.school.domain.discipline.model.IncidentSeverity;

/** Narrows the discipline-incident list; every part is optional. */
public record DisciplineIncidentFilter(String studentPublicId, IncidentSeverity severity, DateWindow dates) {

	public static final DisciplineIncidentFilter NONE = new DisciplineIncidentFilter(null, null, DateWindow.UNBOUNDED);
}
