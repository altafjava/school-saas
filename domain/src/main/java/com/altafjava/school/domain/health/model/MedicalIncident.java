package com.altafjava.school.domain.health.model;

import java.time.LocalDateTime;
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

@Entity
@Table(name = "medical_incidents")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MedicalIncident extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Pii
	@Column(name = "description", nullable = false, length = 1000)
	private String description;

	@Pii
	@Column(name = "treatment_given", length = 1000)
	private String treatmentGiven;

	@Column(name = "guardian_notified", nullable = false)
	private boolean guardianNotified;

	@Column(name = "recorded_by_user_id", nullable = false)
	private Long recordedByUserId;

	public static MedicalIncident record(Long studentId, LocalDateTime occurredAt, String description,
			String treatmentGiven, Long recordedByUserId) {
		return MedicalIncident.builder()
				.studentId(studentId)
				.occurredAt(occurredAt)
				.description(description)
				.treatmentGiven(treatmentGiven)
				.recordedByUserId(recordedByUserId)
				.guardianNotified(false)
				.build();
	}

	public void markGuardianNotified() {
		this.guardianNotified = true;
	}

	/** Clears PHI-grade narrative fields for a GDPR/DPDP erasure request. */
	public void erasePii() {
		this.description = "[erased]";
		this.treatmentGiven = null;
	}
}
