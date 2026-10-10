package com.altafjava.school.application.filter;

/** Narrows the medical-incident list; every part is optional. */
public record MedicalIncidentFilter(String studentPublicId, DateWindow dates) {

	public static final MedicalIncidentFilter NONE = new MedicalIncidentFilter(null, DateWindow.UNBOUNDED);
}
