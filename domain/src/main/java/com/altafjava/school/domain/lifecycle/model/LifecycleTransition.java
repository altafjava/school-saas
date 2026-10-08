package com.altafjava.school.domain.lifecycle.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.TenantEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One step in a person's journey, append-only (never edited or soft-deleted — same evidentiary
 * standing as {@code GradeCorrection}). Before enrollment only {@code admissionId} is set; the
 * enrollment step sets both; afterwards only {@code studentId}. Reading a student's timeline joins
 * the two, so the history is continuous across the applicant → student boundary.
 *
 * <p>
 * {@code effectiveOn} is the business date the change took effect (a withdrawal recorded on Monday
 * for the previous Friday); the row's {@code createdAt} is when it was recorded.
 */
@Entity
@Table(name = "lifecycle_transitions")
@Getter
@SuperBuilder
@NoArgsConstructor
public class LifecycleTransition extends TenantEntity {

	// FK to admissions.id
	@Column(name = "admission_id")
	private Long admissionId;

	// FK to students.id
	@Column(name = "student_id")
	private Long studentId;

	@Column(name = "reason", length = 500)
	private String reason;

	@Column(name = "effective_on", nullable = false)
	private LocalDate effectiveOn;

	// FK to platform users.id — null for public submissions and system-driven transitions.
	@Column(name = "recorded_by_user_id")
	private Long recordedByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_stage", length = 20)
	private LifecycleStage fromStage;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_stage", nullable = false, length = 20)
	private LifecycleStage toStage;

	public static LifecycleTransition record(Long admissionId, Long studentId, LifecycleStage fromStage,
			LifecycleStage toStage, String reason, LocalDate effectiveOn, Long recordedByUserId) {
		if (admissionId == null && studentId == null) {
			throw new BusinessException("A lifecycle transition needs an admission, a student, or both");
		}
		if (toStage == null || toStage == fromStage) {
			throw new BusinessException("A lifecycle transition must change the stage");
		}
		return LifecycleTransition.builder()
				.admissionId(admissionId)
				.studentId(studentId)
				.fromStage(fromStage)
				.toStage(toStage)
				.reason(reason)
				.effectiveOn(effectiveOn != null ? effectiveOn : LocalDate.now())
				.recordedByUserId(recordedByUserId)
				.build();
	}
}
