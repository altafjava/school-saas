package com.altafjava.school.domain.guardian.model;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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
@Table(name = "guardians")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Guardian extends SoftDeletableEntity {

	@Pii
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Pii
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Pii
	@Column(name = "email", length = 255)
	private String email;

	@Pii
	@Column(name = "phone", length = 30)
	private String phone;

	// FK to platform users.id — nullable, set only once this guardian has a login account.
	@Column(name = "user_id")
	private Long userId;

	@Embedded
	private Address address;

	// FK to platform file_metadata.public_id (see Student.photoFilePublicId's Javadoc) — nullable;
	// useful for pickup-authorization/gate-security use cases (visually confirming who is
	// collecting a student), not just a display picture.
	@Column(name = "photo_file_public_id")
	private UUID photoFilePublicId;

	public static Guardian create(String firstName, String lastName, String email, String phone, Long userId) {
		return Guardian.builder()
				.firstName(firstName)
				.lastName(lastName)
				.email(email)
				.phone(phone)
				.userId(userId)
				.build();
	}

	public void linkUserAccount(Long userId) {
		if (this.userId != null) {
			throw new BusinessException("Guardian is already linked to a user account");
		}
		this.userId = userId;
	}

	public void updateAddress(Address address) {
		this.address = Address.copyOf(address);
	}

	public void updatePhone(String phone) {
		this.phone = phone;
	}

	public void updatePhoto(UUID photoFilePublicId) {
		this.photoFilePublicId = photoFilePublicId;
	}

	// GDPR/DPDP erasure (see DomainPiiHandler) — mirrors the platform's own User tombstone
	// strategy: firstName/lastName can't go null (NOT NULL columns) so they get an opaque
	// placeholder, everything else PII-bearing is cleared.
	public void erasePii() {
		this.firstName = "[erased]";
		this.lastName = "[erased]";
		this.email = null;
		this.phone = null;
		this.address = null;
		this.photoFilePublicId = null;
	}
}
