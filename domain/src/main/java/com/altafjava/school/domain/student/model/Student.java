package com.altafjava.school.domain.student.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import com.altafjava.school.domain.common.model.Address;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "students")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Student extends SoftDeletableEntity {

	@Pii
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Pii
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Pii
	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "student_code", nullable = false, length = 50)
	private String studentCode;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	@Enumerated(EnumType.STRING)
	@Column(name = "enrollment_status", nullable = false, length = 30)
	private EnrollmentStatus enrollmentStatus;

	// Distinct from updatedAt — a later, unrelated contact-detail edit must not reset the
	// retention-window clock SchoolDataRetentionHandler measures against this timestamp.
	@Column(name = "enrollment_status_changed_at")
	private Instant enrollmentStatusChangedAt;

	// FK to platform users.id — nullable, set only once this student has their own login account.
	@Column(name = "user_id")
	private Long userId;

	@Pii(type = Pii.PiiType.PHONE)
	@Column(name = "phone", length = 30)
	private String phone;

	@Embedded
	private Address address;

	// FK to platform file_metadata.public_id — the UUID publicId (never the internal surrogate
	// Long id, per the platform's own DTO/identifier convention), nullable, set only once a photo
	// is uploaded via platform's FileStorageService (quota/virus-scan/ownership controls apply,
	// same as any other platform file).
	@Column(name = "photo_file_public_id")
	private UUID photoFilePublicId;

	public static Student create(String studentCode, String firstName, String lastName,
			String email, LocalDate dateOfBirth) {
		return Student.builder()
				.studentCode(studentCode)
				.firstName(firstName)
				.lastName(lastName)
				.email(email)
				.dateOfBirth(dateOfBirth)
				.enrollmentStatus(EnrollmentStatus.ACTIVE)
				.build();
	}

	/** Leaving the school is final: only an enrolled or suspended student can withdraw or transfer. */
	public void withdraw() {
		requireCurrentlyAttending("withdraw");
		moveTo(EnrollmentStatus.WITHDRAWN);
	}

	/**
	 * Distinct from {@link #withdraw()} — a transfer-out to another school, not a generic
	 * withdrawal, so reporting can tell the two apart instead of collapsing every non-graduate exit
	 * into one "withdrawn" bucket.
	 */
	public void transfer() {
		requireCurrentlyAttending("transfer");
		moveTo(EnrollmentStatus.TRANSFERRED);
	}

	public void graduate() {
		if (this.enrollmentStatus != EnrollmentStatus.ACTIVE) {
			throw new BusinessException("Only an active student can graduate, was " + this.enrollmentStatus);
		}
		moveTo(EnrollmentStatus.GRADUATED);
	}

	public void suspend() {
		if (this.enrollmentStatus != EnrollmentStatus.ACTIVE) {
			throw new BusinessException("Only an active student can be suspended, was " + this.enrollmentStatus);
		}
		moveTo(EnrollmentStatus.SUSPENDED);
	}

	public void reinstate() {
		if (this.enrollmentStatus != EnrollmentStatus.SUSPENDED) {
			throw new BusinessException("Only a suspended student can be reinstated, was " + this.enrollmentStatus);
		}
		moveTo(EnrollmentStatus.ACTIVE);
	}

	private void requireCurrentlyAttending(String action) {
		if (this.enrollmentStatus != EnrollmentStatus.ACTIVE && this.enrollmentStatus != EnrollmentStatus.SUSPENDED) {
			throw new BusinessException("Cannot " + action + " a student who is " + this.enrollmentStatus);
		}
	}

	private void moveTo(EnrollmentStatus status) {
		this.enrollmentStatus = status;
		this.enrollmentStatusChangedAt = Instant.now();
	}

	public void updateContactDetails(String firstName, String lastName, String email, LocalDate dateOfBirth) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.dateOfBirth = dateOfBirth;
	}

	// Caller (StudentService) validates the phone against PhoneNumberValidator first — this
	// method just persists an already-validated value.
	public void updatePhone(String phone) {
		this.phone = phone;
	}

	public void updateAddress(Address address) {
		this.address = Address.copyOf(address);
	}

	public void updatePhoto(UUID photoFilePublicId) {
		this.photoFilePublicId = photoFilePublicId;
	}

	// GDPR/DPDP erasure (see DomainPiiHandler) — mirrors the platform's own User tombstone
	// strategy: firstName/lastName can't go null (NOT NULL columns) so they get an opaque
	// placeholder, everything else PII-bearing is cleared. studentCode/dateOfBirth are left
	// intact — operational/academic identifiers, not personal-contact PII.
	public void erasePii() {
		this.firstName = "[erased]";
		this.lastName = "[erased]";
		this.email = null;
		this.phone = null;
		this.address = null;
		this.photoFilePublicId = null;
	}
}
