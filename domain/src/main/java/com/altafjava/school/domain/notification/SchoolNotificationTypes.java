package com.altafjava.school.domain.notification;

import java.util.Set;

/** Notification types owned by school-saas, registered through {@code SchoolPlatformConfigurer}. */
public final class SchoolNotificationTypes {

	public static final String GRADE_PUBLISHED = "GRADE_PUBLISHED";
	public static final String ATTENDANCE_MARKED = "ATTENDANCE_MARKED";
	public static final String FEE_DUE = "FEE_DUE";
	public static final String FEE_OVERDUE = "FEE_OVERDUE";
	public static final String EXAM_SCHEDULED = "EXAM_SCHEDULED";
	public static final String ASSIGNMENT_POSTED = "ASSIGNMENT_POSTED";
	public static final String SUBMISSION_GRADED = "SUBMISSION_GRADED";
	public static final String LOW_ATTENDANCE_ALERT = "LOW_ATTENDANCE_ALERT";
	public static final String APPLICATION_STATUS_CHANGED = "APPLICATION_STATUS_CHANGED";
	public static final String MERIT_LIST_PUBLISHED = "MERIT_LIST_PUBLISHED";
	public static final String LEAVE_REQUESTED = "LEAVE_REQUESTED";
	public static final String LEAVE_APPROVED = "LEAVE_APPROVED";
	public static final String LEAVE_REJECTED = "LEAVE_REJECTED";
	public static final String DISCIPLINE_INCIDENT_RECORDED = "DISCIPLINE_INCIDENT_RECORDED";
	public static final String BOOK_OVERDUE = "BOOK_OVERDUE";
	public static final String EVENT_REGISTRATION_CONFIRMED = "EVENT_REGISTRATION_CONFIRMED";
	public static final String FEE_DEFAULT_RISK = "FEE_DEFAULT_RISK";

	public static final Set<String> ALL = Set.of(GRADE_PUBLISHED, ATTENDANCE_MARKED, FEE_DUE, FEE_OVERDUE,
			EXAM_SCHEDULED, ASSIGNMENT_POSTED, SUBMISSION_GRADED, LOW_ATTENDANCE_ALERT, APPLICATION_STATUS_CHANGED,
			MERIT_LIST_PUBLISHED, LEAVE_REQUESTED, LEAVE_APPROVED, LEAVE_REJECTED, DISCIPLINE_INCIDENT_RECORDED,
			BOOK_OVERDUE, EVENT_REGISTRATION_CONFIRMED, FEE_DEFAULT_RISK);

	private SchoolNotificationTypes() {
	}
}
