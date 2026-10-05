package com.altafjava.school.domain.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Append-only audit trail for {@link HealthRecord} — mirrors {@code GradeCorrection}/
 * {@code AttendanceCorrection}: one row per {@code upsert}, capturing the pre-change values so
 * PHI history is reconstructable instead of silently overwritten. All value columns are
 * {@link Pii} (PHI-grade, more sensitive than ordinary student PII), same as on {@code
 * HealthRecord} itself.
 */
@Entity
@Table(name = "health_record_corrections")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class HealthRecordCorrection extends SoftDeletableEntity {

	// FK to health_records.id
	@Column(name = "health_record_id", nullable = false)
	private Long healthRecordId;

	@Pii
	@Column(name = "old_blood_group", length = 10)
	private String oldBloodGroup;

	@Pii
	@Column(name = "old_allergies", length = 1000)
	private String oldAllergies;

	@Pii
	@Column(name = "old_conditions", length = 1000)
	private String oldConditions;

	@Column(name = "old_immunizations", length = 1000)
	private String oldImmunizations;

	@Pii
	@Column(name = "new_blood_group", length = 10)
	private String newBloodGroup;

	@Pii
	@Column(name = "new_allergies", length = 1000)
	private String newAllergies;

	@Pii
	@Column(name = "new_conditions", length = 1000)
	private String newConditions;

	@Column(name = "new_immunizations", length = 1000)
	private String newImmunizations;

	public static HealthRecordCorrection record(Long healthRecordId, String oldBloodGroup, String oldAllergies,
			String oldConditions, String oldImmunizations, String newBloodGroup, String newAllergies,
			String newConditions, String newImmunizations) {
		return HealthRecordCorrection.builder()
				.healthRecordId(healthRecordId)
				.oldBloodGroup(oldBloodGroup)
				.oldAllergies(oldAllergies)
				.oldConditions(oldConditions)
				.oldImmunizations(oldImmunizations)
				.newBloodGroup(newBloodGroup)
				.newAllergies(newAllergies)
				.newConditions(newConditions)
				.newImmunizations(newImmunizations)
				.build();
	}
}
