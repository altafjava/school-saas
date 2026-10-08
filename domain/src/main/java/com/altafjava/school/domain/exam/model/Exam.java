package com.altafjava.school.domain.exam.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
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

	public static final BigDecimal FULL_WEIGHTAGE = BigDecimal.valueOf(100);

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	// FK to subjects.id
	@Column(name = "subject_id", nullable = false)
	private Long subjectId;

	// FK to classrooms.id
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt;

	@Column(name = "max_marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal maxMarks;

	// FK to terms.id — nullable: existing exams predate this field and have no reliable source
	// to backfill from (ReportCardService derives term membership from scheduledAt, not an FK).
	@Column(name = "term_id")
	private Long termId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ExamStatus status;

	// FK to exam_type_definitions.id — nullable only at the DB level; create()/ExamService
	// require and validate a real value.
	@Column(name = "exam_type_id")
	private Long examTypeId;

	// Percentage contribution to the subject's result for the term; the exams of one subject in one
	// term together weigh at most 100.
	@Column(name = "weightage", nullable = false, precision = 5, scale = 2)
	private BigDecimal weightage;

	// Students and guardians see no grade of this exam until results are published.
	@Column(name = "results_published_at")
	private Instant resultsPublishedAt;

	@Column(name = "results_published_by", length = 100)
	private String resultsPublishedBy;

	public static Exam create(String title, Long subjectId, Long classroomId,
			LocalDateTime scheduledAt, BigDecimal maxMarks, Long termId, Long examTypeId, BigDecimal weightage) {
		requireValidWeightage(weightage);
		return Exam.builder()
				.title(title)
				.subjectId(subjectId)
				.classroomId(classroomId)
				.scheduledAt(scheduledAt)
				.maxMarks(maxMarks)
				.termId(termId)
				.examTypeId(examTypeId)
				.weightage(weightage)
				.status(ExamStatus.SCHEDULED)
				.build();
	}

	private static void requireValidWeightage(BigDecimal weightage) {
		if (weightage == null || weightage.signum() <= 0 || weightage.compareTo(FULL_WEIGHTAGE) > 0) {
			throw new BusinessException("Exam weightage must be above 0 and at most 100");
		}
	}

	public void reschedule(LocalDateTime scheduledAt) {
		this.scheduledAt = scheduledAt;
	}

	public void assignTerm(Long termId) {
		this.termId = termId;
	}

	public void reweight(BigDecimal weightage) {
		if (isResultsPublished()) {
			throw new BusinessException("Weightage cannot change after results are published");
		}
		requireValidWeightage(weightage);
		this.weightage = weightage;
	}

	public boolean isResultsPublished() {
		return resultsPublishedAt != null;
	}

	public void publishResults(String publishedBy) {
		if (this.status != ExamStatus.COMPLETED) {
			throw new BusinessException("Results can only be published for a completed exam");
		}
		if (isResultsPublished()) {
			throw new BusinessException("Results are already published");
		}
		this.resultsPublishedAt = Instant.now();
		this.resultsPublishedBy = publishedBy;
	}

	public void withdrawResults() {
		if (!isResultsPublished()) {
			throw new BusinessException("Results are not published");
		}
		this.resultsPublishedAt = null;
		this.resultsPublishedBy = null;
	}

	public void complete() {
		if (this.status == ExamStatus.CANCELLED) {
			throw new BusinessException("Cannot complete a cancelled exam");
		}
		if (this.status == ExamStatus.COMPLETED) {
			throw new BusinessException("Exam is already completed");
		}
		this.status = ExamStatus.COMPLETED;
	}

	public void cancel() {
		if (this.status == ExamStatus.COMPLETED) {
			throw new BusinessException("Cannot cancel a completed exam");
		}
		if (this.status == ExamStatus.CANCELLED) {
			throw new BusinessException("Exam is already cancelled");
		}
		this.status = ExamStatus.CANCELLED;
	}
}
