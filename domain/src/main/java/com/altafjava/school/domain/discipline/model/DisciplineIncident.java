package com.altafjava.school.domain.discipline.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "discipline_incidents")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DisciplineIncident extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "reported_by_teacher_id", nullable = false)
	private Long reportedByTeacherId;

	@Column(name = "incident_date", nullable = false)
	private LocalDate incidentDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity", nullable = false, length = 20)
	private IncidentSeverity severity;

	@Pii
	@Column(name = "description", nullable = false, length = 1000)
	private String description;

	@Pii
	@Column(name = "action_taken", length = 1000)
	private String actionTaken;

	@Column(name = "guardian_notified", nullable = false)
	private boolean guardianNotified;

	public static DisciplineIncident report(Long studentId, Long reportedByTeacherId, LocalDate incidentDate,
			IncidentSeverity severity, String description) {
		return DisciplineIncident.builder()
				.studentId(studentId)
				.reportedByTeacherId(reportedByTeacherId)
				.incidentDate(incidentDate)
				.severity(severity)
				.description(description)
				.guardianNotified(false)
				.build();
	}

	public void recordAction(String actionTaken) {
		this.actionTaken = actionTaken;
	}

	public void markGuardianNotified() {
		this.guardianNotified = true;
	}

	/** Clears narrative fields identifying the student's behavior for a GDPR/DPDP erasure request. */
	public void erasePii() {
		this.description = "[erased]";
		this.actionTaken = null;
	}
}
