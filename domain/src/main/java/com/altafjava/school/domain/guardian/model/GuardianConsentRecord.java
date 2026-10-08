package com.altafjava.school.domain.guardian.model;

import java.time.Instant;
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
 * A guardian's own consent to a specific category of processing of their linked minor student's
 * data — mirrors platform's {@code ConsentRecord} shape, but for the "granted by a guardian on
 * behalf of a subject who is not the consenting platform user" case {@code ConsentRecord} cannot
 * represent (a {@code Student} frequently has no platform {@code User} account of its own). Never
 * soft-deleted — a permanent, append-only evidentiary record, same as {@code ConsentRecord}.
 */
@Entity
@Table(name = "guardian_consent_records")
@Getter
@SuperBuilder
@NoArgsConstructor
public class GuardianConsentRecord extends TenantEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "guardian_id", nullable = false)
	private Long guardianId;

	@Column(name = "granted", nullable = false)
	private boolean granted;

	@Column(name = "granted_at")
	private Instant grantedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "policy_version")
	private String policyVersion;

	@Enumerated(EnumType.STRING)
	@Column(name = "consent_type", nullable = false)
	private GuardianConsentType consentType;

	public static GuardianConsentRecord create(Long studentId, Long guardianId, GuardianConsentType consentType) {
		return GuardianConsentRecord.builder()
				.studentId(studentId)
				.guardianId(guardianId)
				.consentType(consentType)
				.granted(false)
				.build();
	}

	public void grant(String policyVersion) {
		this.granted = true;
		this.grantedAt = Instant.now();
		this.revokedAt = null;
		this.policyVersion = policyVersion;
	}

	public void revoke() {
		if (!this.granted) {
			throw new BusinessException("Consent of type " + consentType + " is not currently granted");
		}
		this.granted = false;
		this.revokedAt = Instant.now();
	}
}
