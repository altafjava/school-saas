package com.altafjava.school.domain.counseling.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * {@code counselorTeacherId} references {@code Teacher} (school-saas's staff/employee entity),
 * mirroring {@code VisitorLog.hostTeacherId} — every other cross-entity reference in this codebase
 * resolves through a repository via a client-supplied public id, and {@code Teacher} is the one
 * staff-facing entity in school-saas's own domain boundary with that lookup already available.
 * {@code notes} is {@code @Pii}-flagged (masked in logs, encrypted at rest), matching
 * {@code HealthRecord}'s posture for confidential student data.
 */
@Entity
@Table(name = "counseling_sessions")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CounselingSession extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "counselor_teacher_id", nullable = false)
	private Long counselorTeacherId;

	@Column(name = "session_date", nullable = false)
	private LocalDate sessionDate;

	@Pii
	@Column(name = "notes", length = 2000)
	private String notes;

	@Column(name = "follow_up_required", nullable = false)
	private boolean followUpRequired;

	public static CounselingSession schedule(Long studentId, Long counselorTeacherId, LocalDate sessionDate,
			String notes, boolean followUpRequired) {
		return CounselingSession.builder()
				.studentId(studentId)
				.counselorTeacherId(counselorTeacherId)
				.sessionDate(sessionDate)
				.notes(notes)
				.followUpRequired(followUpRequired)
				.build();
	}

	public void updateNotes(String notes, boolean followUpRequired) {
		this.notes = notes;
		this.followUpRequired = followUpRequired;
	}

	/** Clears counseling notes for a GDPR/DPDP erasure request. */
	public void erasePii() {
		this.notes = null;
	}
}
