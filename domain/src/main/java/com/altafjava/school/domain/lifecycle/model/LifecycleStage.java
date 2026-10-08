package com.altafjava.school.domain.lifecycle.model;

import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.student.model.EnrollmentStatus;

/**
 * One vocabulary for a person's whole journey through the school — applicant, student, alumnus —
 * so a single timeline can span Admission → Enrollment → Alumni instead of three status fields
 * that each only know their own phase.
 */
public enum LifecycleStage {
	SUBMITTED, UNDER_REVIEW, WAITLISTED, APPROVED, REJECTED, ENROLLED, SUSPENDED, WITHDRAWN, TRANSFERRED, GRADUATED, ALUMNI;

	public static LifecycleStage of(AdmissionStatus status) {
		return switch (status) {
			case SUBMITTED -> SUBMITTED;
			case UNDER_REVIEW -> UNDER_REVIEW;
			case WAITLISTED -> WAITLISTED;
			case APPROVED -> APPROVED;
			case REJECTED -> REJECTED;
			case ENROLLED -> ENROLLED;
		};
	}

	public static LifecycleStage of(EnrollmentStatus status) {
		return switch (status) {
			case ACTIVE -> ENROLLED;
			case SUSPENDED -> SUSPENDED;
			case WITHDRAWN -> WITHDRAWN;
			case TRANSFERRED -> TRANSFERRED;
			case GRADUATED -> GRADUATED;
		};
	}
}
