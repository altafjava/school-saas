package com.altafjava.school.domain.lms.model;

import java.math.BigDecimal;
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
@Table(name = "submissions")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Submission extends SoftDeletableEntity {

	// FK to assignments.id
	@Column(name = "assignment_id", nullable = false)
	private Long assignmentId;

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to teachers.id — the grading teacher
	@Column(name = "graded_by")
	private Long gradedBy;

	@Column(name = "submitted_at", nullable = false)
	private LocalDateTime submittedAt;

	// Object storage key (platform StorageService) for an optional single attachment.
	@Column(name = "storage_key", length = 500)
	private String storageKey;

	@Column(name = "text_content", columnDefinition = "TEXT")
	private String textContent;

	@Column(name = "marks_obtained", precision = 10, scale = 2)
	private BigDecimal marksObtained;

	@Column(name = "feedback", columnDefinition = "TEXT")
	private String feedback;

	@Column(name = "graded_at")
	private LocalDateTime gradedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SubmissionStatus status;

	public static Submission submit(Long assignmentId, Long studentId, String storageKey, String textContent,
			boolean isLate) {
		return Submission.builder()
				.assignmentId(assignmentId)
				.studentId(studentId)
				.storageKey(storageKey)
				.textContent(textContent)
				.submittedAt(LocalDateTime.now())
				.status(isLate ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED)
				.build();
	}

	public void grade(BigDecimal marks, String feedback, Long gradedByTeacherId) {
		if (this.status == SubmissionStatus.GRADED) {
			throw new BusinessException("Submission is already graded");
		}
		this.marksObtained = marks;
		this.feedback = feedback;
		this.gradedBy = gradedByTeacherId;
		this.gradedAt = LocalDateTime.now();
		this.status = SubmissionStatus.GRADED;
	}
}
