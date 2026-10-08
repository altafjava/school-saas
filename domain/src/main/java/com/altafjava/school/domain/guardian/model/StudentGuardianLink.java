package com.altafjava.school.domain.guardian.model;

import java.time.Instant;
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

	@Column(name = "primary_contact", nullable = false)
	private boolean primaryContact;

	@Column(name = "consent_given_at")
	private Instant consentGivenAt;

	// Distinct from primaryContact and from guardianship itself: a grandparent can be pickup-
	// authorized without being a legal guardian. Defaults to false — pickup is always an explicit grant.
	@Column(name = "authorized_for_pickup", nullable = false)
	private boolean authorizedForPickup;

	// Bars pickup and own-child data access without unlinking the guardian (e.g. a non-custodial
	// parent linked for record-keeping). Always wins over authorizedForPickup.
	@Column(name = "custody_restricted", nullable = false)
	private boolean custodyRestricted;

	@Column(name = "custody_restriction_note", length = 500)
	private String custodyRestrictionNote;

	@Enumerated(EnumType.STRING)
	@Column(name = "relationship_type", nullable = false, length = 30)
	private RelationshipType relationshipType;

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

	// Custody restriction is checked first so it can never be shadowed by a stale pickup flag.
	public PickupDecision pickupDecision() {
		if (custodyRestricted) {
			return PickupDecision.CUSTODY_RESTRICTED;
		}
		return authorizedForPickup ? PickupDecision.AUTHORIZED : PickupDecision.NOT_AUTHORIZED;
	}

	public void authorizePickup() {
		if (custodyRestricted) {
			throw new BusinessException("Cannot authorize pickup while custody is restricted");
		}
		this.authorizedForPickup = true;
	}

	public void revokePickupAuthorization() {
		this.authorizedForPickup = false;
	}

	public void restrictCustody(String note) {
		this.custodyRestricted = true;
		this.custodyRestrictionNote = note;
		this.authorizedForPickup = false;
	}

	public void liftCustodyRestriction() {
		this.custodyRestricted = false;
		this.custodyRestrictionNote = null;
	}
}
