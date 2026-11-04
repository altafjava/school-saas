package com.altafjava.school.domain.exam.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.school.domain.error.AcademicErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "exams")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Exam extends SoftDeletableEntity {

	@PrePersist
	@PreUpdate
	private void assertConsistent() {
		if (weightage != null && (weightage.signum() <= 0 || weightage.compareTo(BigDecimal.valueOf(100)) > 0)) {
			throw new BusinessException(PlatformErrorCode.INTERNAL_ERROR,
					"Exam weightage must be above 0 and at most 100");
		}
		requireValidDuration(durationMinutes);
	}

	public static final BigDecimal FULL_WEIGHTAGE = BigDecimal.valueOf(100);
	public static final int MIN_DURATION_MINUTES = 5;
	public static final int MAX_DURATION_MINUTES = 720;

	// FK to subjects.id
	@Column(name = "subject_id", nullable = false)
	private Long subjectId;

	// FK to classrooms.id
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	// FK to terms.id — nullable: existing exams predate this field and have no reliable source
	// to backfill from (ReportCardService derives term membership from scheduledAt, not an FK).
	@Column(name = "term_id")
	private Long termId;

	// FK to exam_type_definitions.id — nullable only at the DB level; create()/ExamService
	// require and validate a real value.
	@Column(name = "exam_type_id")
	private Long examTypeId;

	// FK to venues.id — optional; an exam without a venue takes part in no room-clash check.
	@Column(name = "venue_id")
	private Long venueId;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Column(name = "max_marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal maxMarks;

	// Percentage contribution to the subject's result for the term; the exams of one subject in one
	// term together weigh at most 100.
	@Column(name = "weightage", nullable = false, precision = 5, scale = 2)
	private BigDecimal weightage;

	// Students and guardians see no grade of this exam until results are published.
	@Column(name = "results_published_at")
	private Instant resultsPublishedAt;

	@Column(name = "results_published_by", length = 100)
	private String resultsPublishedBy;

	// Students and guardians see the exam's date only once the classroom's schedule is published.
	@Column(name = "schedule_published_at")
	private Instant schedulePublishedAt;

	@Column(name = "marks_verified_at")
	private Instant marksVerifiedAt;

	@Column(name = "marks_verified_by", length = 100)
	private String marksVerifiedBy;

	@Column(name = "marks_locked_at")
	private Instant marksLockedAt;

	@Column(name = "marks_locked_by", length = 100)
	private String marksLockedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ExamStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "marks_stage", nullable = false, length = 20)
	private MarksStage marksStage;

	public static Exam create(String title, Long subjectId, Long classroomId, ExamSlot slot, BigDecimal maxMarks,
			Long termId, Long examTypeId, BigDecimal weightage) {
		requireValidWeightage(weightage);
		requireValidDuration(slot.durationMinutes());
		return Exam.builder()
				.title(title)
				.subjectId(subjectId)
				.classroomId(classroomId)
				.scheduledAt(slot.startsAt())
				.durationMinutes(slot.durationMinutes())
				.venueId(slot.venueId())
				.maxMarks(maxMarks)
				.termId(termId)
				.examTypeId(examTypeId)
				.weightage(weightage)
				.status(ExamStatus.SCHEDULED)
				.marksStage(MarksStage.ENTERED)
				.build();
	}

	private static void requireValidDuration(int durationMinutes) {
		if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
			throw new BusinessException(AcademicErrorCode.EXAM_DURATION_INVALID,
					"An exam must last between " + MIN_DURATION_MINUTES + " and "
							+ MAX_DURATION_MINUTES + " minutes",
					Map.of("min", MIN_DURATION_MINUTES, "max", MAX_DURATION_MINUTES));
		}
	}

	public ExamSlot slot() {
		return new ExamSlot(scheduledAt, durationMinutes, venueId);
	}

	public LocalDateTime endsAt() {
		return scheduledAt.plusMinutes(durationMinutes);
	}

	/** Two exams clash when their times overlap and they share a classroom or a venue. */
	public boolean clashesWith(Exam other) {
		if (other.status == ExamStatus.CANCELLED || other.getId() != null && other.getId().equals(getId())) {
			return false;
		}
		boolean overlap = scheduledAt.isBefore(other.endsAt()) && other.scheduledAt.isBefore(endsAt());
		boolean sameRoom = venueId != null && venueId.equals(other.venueId);
		return overlap && (classroomId.equals(other.classroomId) || sameRoom);
	}

	public boolean isSchedulePublished() {
		return schedulePublishedAt != null;
	}

	public void publishSchedule() {
		if (status != ExamStatus.SCHEDULED) {
			throw new BusinessException(AcademicErrorCode.EXAM_STATUS_INVALID,
					"Only a scheduled exam can be published to families");
		}
		if (isSchedulePublished()) {
			throw new BusinessException(AcademicErrorCode.EXAM_SCHEDULE_ALREADY_PUBLISHED,
					"The exam's schedule is already published");
		}
		this.schedulePublishedAt = Instant.now();
	}

	/** Recording or correcting a mark is allowed only while the marks are still being entered. */
	public void requireMarksOpen() {
		if (marksStage != MarksStage.ENTERED) {
			throw new BusinessException(AcademicErrorCode.EXAM_MARKS_LOCKED,
					"The marks of this exam are " + marksStage.name().toLowerCase()
							+ " and can no longer change",
					Map.of("stage", marksStage));
		}
	}

	public void verifyMarks(String verifiedBy) {
		if (status != ExamStatus.COMPLETED) {
			throw new BusinessException(AcademicErrorCode.EXAM_STATUS_INVALID,
					"Marks can be verified only once the exam is completed");
		}
		requireMarksStage(MarksStage.ENTERED, "verify");
		this.marksStage = MarksStage.VERIFIED;
		this.marksVerifiedAt = Instant.now();
		this.marksVerifiedBy = verifiedBy;
	}

	public void returnMarksForCorrection() {
		requireMarksStage(MarksStage.VERIFIED, "return");
		reopenMarks();
	}

	public void lockMarks(String lockedBy) {
		requireMarksStage(MarksStage.VERIFIED, "lock");
		this.marksStage = MarksStage.LOCKED;
		this.marksLockedAt = Instant.now();
		this.marksLockedBy = lockedBy;
	}

	public void unlockMarks() {
		requireMarksStage(MarksStage.LOCKED, "unlock");
		if (isResultsPublished()) {
			throw new BusinessException(AcademicErrorCode.RESULTS_PUBLISHED,
					"Withdraw the published results before unlocking the marks");
		}
		reopenMarks();
	}

	private void reopenMarks() {
		this.marksStage = MarksStage.ENTERED;
		this.marksVerifiedAt = null;
		this.marksVerifiedBy = null;
		this.marksLockedAt = null;
		this.marksLockedBy = null;
	}

	private void requireMarksStage(MarksStage required, String action) {
		if (marksStage != required) {
			throw new BusinessException(AcademicErrorCode.EXAM_MARKS_LOCKED,
					"Cannot " + action + " marks that are " + marksStage.name().toLowerCase(),
					Map.of("stage", marksStage));
		}
	}

	private static void requireValidWeightage(BigDecimal weightage) {
		if (weightage == null || weightage.signum() <= 0 || weightage.compareTo(FULL_WEIGHTAGE) > 0) {
			throw new BusinessException(AcademicErrorCode.EXAM_WEIGHTAGE_INVALID,
					"Exam weightage must be above 0 and at most 100");
		}
	}

	public void reschedule(ExamSlot slot) {
		if (status != ExamStatus.SCHEDULED) {
			throw new BusinessException(AcademicErrorCode.EXAM_STATUS_INVALID, "Only a scheduled exam can be moved");
		}
		requireValidDuration(slot.durationMinutes());
		this.scheduledAt = slot.startsAt();
		this.durationMinutes = slot.durationMinutes();
		this.venueId = slot.venueId();
	}

	public void assignTerm(Long termId) {
		this.termId = termId;
	}

	public void reweight(BigDecimal weightage) {
		if (isResultsPublished()) {
			throw new BusinessException(AcademicErrorCode.RESULTS_PUBLISHED,
					"Weightage cannot change after results are published");
		}
		requireValidWeightage(weightage);
		this.weightage = weightage;
	}

	public boolean isResultsPublished() {
		return resultsPublishedAt != null;
	}

	public void publishResults(String publishedBy) {
		if (this.status != ExamStatus.COMPLETED) {
			throw new BusinessException(AcademicErrorCode.EXAM_STATUS_INVALID,
					"Results can only be published for a completed exam");
		}
		if (marksStage != MarksStage.LOCKED) {
			throw new BusinessException(AcademicErrorCode.MARKS_NOT_VERIFIED,
					"Results can only be published once the marks are verified and locked");
		}
		if (isResultsPublished()) {
			throw new BusinessException(AcademicErrorCode.RESULTS_PUBLISHED, "Results are already published");
		}
		this.resultsPublishedAt = Instant.now();
		this.resultsPublishedBy = publishedBy;
	}

	public void withdrawResults() {
		if (!isResultsPublished()) {
			throw new BusinessException(AcademicErrorCode.RESULTS_NOT_PUBLISHED, "Results are not published");
		}
		this.resultsPublishedAt = null;
		this.resultsPublishedBy = null;
	}

	public void complete() {
		if (this.status == ExamStatus.CANCELLED) {
			throw new BusinessException(AcademicErrorCode.EXAM_STATUS_INVALID, "Cannot complete a cancelled exam");
		}
		if (this.status == ExamStatus.COMPLETED) {
			throw new BusinessException(AcademicErrorCode.EXAM_STATUS_INVALID, "Exam is already completed");
		}
		this.status = ExamStatus.COMPLETED;
	}

	public void cancel() {
		if (this.status == ExamStatus.COMPLETED) {
			throw new BusinessException(AcademicErrorCode.EXAM_STATUS_INVALID, "Cannot cancel a completed exam");
		}
		if (this.status == ExamStatus.CANCELLED) {
			throw new BusinessException(AcademicErrorCode.EXAM_STATUS_INVALID, "Exam is already cancelled");
		}
		this.status = ExamStatus.CANCELLED;
	}
}
