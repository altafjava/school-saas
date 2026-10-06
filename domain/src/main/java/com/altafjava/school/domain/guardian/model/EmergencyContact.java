package com.altafjava.school.domain.guardian.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Someone to call about a student in an emergency — deliberately not a {@link Guardian}: a
 * neighbour or relative with no legal relationship and no platform login is still a valid contact.
 * {@code relationship} is a free-text label for the same reason. Lower {@code priority} is called first.
 */
@Entity
@Table(name = "emergency_contacts")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class EmergencyContact extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Pii
	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "relationship", nullable = false, length = 100)
	private String relationship;

	@Pii
	@Column(name = "phone", nullable = false, length = 30)
	private String phone;

	@Pii
	@Column(name = "alternate_phone", length = 30)
	private String alternatePhone;

	@Column(name = "priority", nullable = false)
	private int priority;

	public static EmergencyContact create(Long studentId, String name, String relationship, String phone,
			String alternatePhone, int priority) {
		if (priority < 1) {
			throw new BusinessException("Emergency contact priority must be 1 or greater");
		}
		return EmergencyContact.builder()
				.studentId(studentId)
				.name(name)
				.relationship(relationship)
				.phone(phone)
				.alternatePhone(alternatePhone)
				.priority(priority)
				.build();
	}

	public void update(String name, String relationship, String phone, String alternatePhone, int priority) {
		if (priority < 1) {
			throw new BusinessException("Emergency contact priority must be 1 or greater");
		}
		this.name = name;
		this.relationship = relationship;
		this.phone = phone;
		this.alternatePhone = alternatePhone;
		this.priority = priority;
	}

	// GDPR/DPDP erasure — NOT NULL columns get an opaque placeholder, optional ones are cleared.
	public void erasePii() {
		this.name = "[erased]";
		this.phone = "[erased]";
		this.alternatePhone = null;
	}
}
