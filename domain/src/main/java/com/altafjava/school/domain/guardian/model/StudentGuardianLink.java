package com.altafjava.school.domain.guardian.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "student_guardian_links")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class StudentGuardianLink extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to guardians.id
	@Column(name = "guardian_id", nullable = false)
	private Long guardianId;

	@Enumerated(EnumType.STRING)
	@Column(name = "relationship_type", nullable = false, length = 30)
	private RelationshipType relationshipType;

	@Column(name = "primary_contact", nullable = false)
	private boolean primaryContact;

	@Column(name = "consent_given_at")
	private Instant consentGivenAt;

	public static StudentGuardianLink create(Long studentId, Long guardianId, RelationshipType relationshipType,
			boolean primaryContact) {
		return StudentGuardianLink.builder()
				.studentId(studentId)
				.guardianId(guardianId)
				.relationshipType(relationshipType)
				.primaryContact(primaryContact)
				.build();
	}

	public void giveConsent() {
		this.consentGivenAt = Instant.now();
	}

	public void revokeConsent() {
		this.consentGivenAt = null;
	}
}
