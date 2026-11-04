package com.altafjava.school.domain.event.model;

import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.school.domain.error.SchoolErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "event_registrations")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EventRegistration extends SoftDeletableEntity {

	@Column(name = "event_id", nullable = false)
	private Long eventId;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private EventRegistrationStatus status;

	public static EventRegistration register(Long eventId, Long studentId) {
		return EventRegistration.builder()
				.eventId(eventId)
				.studentId(studentId)
				.status(EventRegistrationStatus.REGISTERED)
				.build();
	}

	public void cancel() {
		if (this.status != EventRegistrationStatus.REGISTERED) {
			throw new BusinessException(SchoolErrorCode.EVENT_REGISTRATION_STATUS_INVALID,
					"Cannot cancel a registration in status " + this.status, Map.of("status", this.status));
		}
		this.status = EventRegistrationStatus.CANCELLED;
	}

	public void markAttended() {
		if (this.status != EventRegistrationStatus.REGISTERED) {
			throw new BusinessException(SchoolErrorCode.EVENT_REGISTRATION_STATUS_INVALID,
					"Cannot mark attendance for a registration in status " + this.status,
					Map.of("status", this.status));
		}
		this.status = EventRegistrationStatus.ATTENDED;
	}
}
