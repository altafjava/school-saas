package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/** Error codes for classrooms, exams, grading, timetable and assignments; each is part of the public API contract. */
public enum AcademicErrorCode implements ErrorCode {

	NO_CURRENT_ACADEMIC_YEAR(409, "The school has no current academic year."), //
	EXAM_CLASH(409, "The exam overlaps another exam of the same classroom or room."), //
	EXAM_HAS_NO_MARKS(409, "An exam with no marks recorded cannot be verified."), //
	EXAM_TIMETABLE_NOTHING_TO_PUBLISH(409, "The classroom has no scheduled exam waiting to be published."), //
	STUDENT_ALREADY_ENROLLED(409, "The student already has a classroom in this academic year."), //
	CLASSROOM_FULL(409, "The classroom is at full capacity."), //
	ROLL_NUMBER_TAKEN(409, "Another student in the classroom has this roll number."), //
	RESULTS_HAVE_NO_GRADES(409, "Results cannot be published before any grade is recorded."), //
	TERM_WEIGHTAGE_EXCEEDED(400, "The subject's exams in this term would weigh more than 100%."), //
	EXAM_CANCELLED(409, "The exam is cancelled."), //
	DEFAULT_GRADING_SCALE(409, "The school's default grading scale cannot be deactivated."), //
	NO_DEFAULT_GRADING_SCALE(409, "The school has no default grading scale."), //
	GRADING_SCALE_INCOMPLETE(400, "A grading scale needs thresholds covering down to 0%."), //
	PERIOD_TIMES_INVALID(400, "A period must start before it ends."), //
	PROMOTION_STUDENT_NOT_IN_CLASSROOM(400, "A student in the promotion is not in the source classroom."), //
	PROMOTION_STUDENT_NOT_ACTIVE(409, "Only an active student can be promoted."), //
	PROMOTION_TARGET_REQUIRED(400, "Promoting a student needs a target classroom."), //
	PROMOTION_TARGET_NOT_LATER(400, "The target classroom must belong to a later academic year."), //
	SUBMISSION_EXISTS(409, "You have already submitted this assignment."), //
	SUBSTITUTE_ALREADY_ASSIGNED(409, "A substitute is already assigned to this slot on that date."), //
	SUBSTITUTE_NOT_FREE(409, "The substitute is not free at that time."), //
	SUBSTITUTION_WRONG_DAY(400, "The date is not on the slot's weekday."), //
	TIMETABLE_CLASSROOM_BUSY(409, "The classroom already has a lesson in this period."), //
	TIMETABLE_TEACHER_BUSY(409, "The teacher already has a lesson in this period."), //
	VENUE_INACTIVE(409, "The venue is not in use."), //
	TIMETABLE_VENUE_BUSY(409, "The venue is already booked in this period."), //
	EXAM_DURATION_INVALID(400, "The exam's length is outside the allowed range."), //
	EXAM_STATUS_INVALID(409, "The exam is not in a status that allows this change."), //
	EXAM_SCHEDULE_ALREADY_PUBLISHED(409, "The exam's schedule is already published."), //
	EXAM_MARKS_LOCKED(409, "The exam's marks can no longer change at this stage."), //
	RESULTS_PUBLISHED(409, "Withdraw the published results first."), //
	EXAM_WEIGHTAGE_INVALID(400, "Exam weightage must be above 0 and at most 100."), //
	MARKS_NOT_VERIFIED(409, "Results can be published only once the marks are verified and locked."), //
	RESULTS_NOT_PUBLISHED(409, "The results are not published."), //
	SUBMISSION_ALREADY_GRADED(409, "The submission is already graded."), //
	SUBSTITUTION_DATE_PASSED(400, "A substitute cannot be assigned for a date that has passed."), //
	SUBSTITUTE_IS_REGULAR_TEACHER(400, "The substitute must be someone other than the regular teacher."), //
	SUBSTITUTION_ALREADY_CANCELLED(409, "The substitution is already cancelled."), //
	SUBSTITUTION_IN_PAST(409, "A substitution that has already taken place cannot be cancelled."), //
	VENUE_CAPACITY_INVALID(400, "A venue's capacity must be positive.");

	private final int httpStatus;
	private final String description;

	AcademicErrorCode(int httpStatus, String description) {
		this.httpStatus = httpStatus;
		this.description = description;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public int httpStatus() {
		return httpStatus;
	}

	@Override
	public String description() {
		return description;
	}
}
