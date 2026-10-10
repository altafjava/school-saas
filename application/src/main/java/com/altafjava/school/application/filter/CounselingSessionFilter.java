package com.altafjava.school.application.filter;

/** Narrows the counselling-session list; every part is optional. */
public record CounselingSessionFilter(String studentPublicId, DateWindow dates, Boolean followUpRequired) {

	public static final CounselingSessionFilter NONE = new CounselingSessionFilter(null, DateWindow.UNBOUNDED, null);
}
