package com.altafjava.school.domain.health.model;

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
 * One updatable record per student — simpler than versioning history, matching the plan's
 * "simpler is fine" guidance. {@code bloodGroup}, {@code allergies} and {@code conditions} are
 * flagged {@link Pii} (masked in logs, encrypted at rest) since they are health/PHI-grade data,
 * more sensitive than ordinary student PII.
 */
@Entity
@Table(name = "health_records")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class HealthRecord extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false, unique = true)
	private Long studentId;

	@Pii
	@Column(name = "blood_group", length = 10)
	private String bloodGroup;

	@Pii
	@Column(name = "allergies", length = 1000)
	private String allergies;

	@Pii
	@Column(name = "conditions", length = 1000)
	private String conditions;

	@Column(name = "immunizations", length = 1000)
	private String immunizations;

	public static HealthRecord create(Long studentId, String bloodGroup, String allergies, String conditions,
			String immunizations) {
		return HealthRecord.builder()
				.studentId(studentId)
				.bloodGroup(bloodGroup)
				.allergies(allergies)
				.conditions(conditions)
				.immunizations(immunizations)
				.build();
	}

	public void update(String bloodGroup, String allergies, String conditions, String immunizations) {
		this.bloodGroup = bloodGroup;
		this.allergies = allergies;
		this.conditions = conditions;
		this.immunizations = immunizations;
	}

	/** Clears PHI-grade fields for a GDPR/DPDP erasure request — see {@code Student#erasePii}. */
	public void erasePii() {
		this.bloodGroup = null;
		this.allergies = null;
		this.conditions = null;
		this.immunizations = null;
	}
}
