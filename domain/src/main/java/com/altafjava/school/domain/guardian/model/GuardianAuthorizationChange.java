package com.altafjava.school.domain.guardian.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.altafjava.platform.core.model.TenantEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Append-only history of every pickup-authorization / custody-restriction change on a
 * {@link StudentGuardianLink} — a child-safety decision must stay reconstructable ("who allowed
 * this adult to collect this child, and when"). Never soft-deleted, same as
 * {@link GuardianConsentRecord}.
 */
@Entity
@Table(name = "guardian_authorization_changes")
@Getter
@SuperBuilder
@NoArgsConstructor
public class GuardianAuthorizationChange extends TenantEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "guardian_id", nullable = false)
	private Long guardianId;

	@Column(name = "old_authorized_for_pickup", nullable = false)
	private boolean oldAuthorizedForPickup;

	@Column(name = "new_authorized_for_pickup", nullable = false)
	private boolean newAuthorizedForPickup;

	@Column(name = "old_custody_restricted", nullable = false)
	private boolean oldCustodyRestricted;

	@Column(name = "new_custody_restricted", nullable = false)
	private boolean newCustodyRestricted;

	@Column(name = "note", length = 500)
	private String note;

	// FK to platform users.id — the staff member who made the change.
	@Column(name = "changed_by_user_id")
	private Long changedByUserId;

	public static GuardianAuthorizationChange record(Long studentId, Long guardianId, boolean oldAuthorizedForPickup,
			boolean oldCustodyRestricted, StudentGuardianLink after, String note, Long changedByUserId) {
		return GuardianAuthorizationChange.builder()
				.studentId(studentId)
				.guardianId(guardianId)
				.oldAuthorizedForPickup(oldAuthorizedForPickup)
				.newAuthorizedForPickup(after.isAuthorizedForPickup())
				.oldCustodyRestricted(oldCustodyRestricted)
				.newCustodyRestricted(after.isCustodyRestricted())
				.note(note)
				.changedByUserId(changedByUserId)
				.build();
	}
}
