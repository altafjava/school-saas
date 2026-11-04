package com.altafjava.school.domain.hostel.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "room_allocations")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class RoomAllocation extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "allocated_from", nullable = false)
	private LocalDate allocatedFrom;

	@Column(name = "allocated_until")
	private LocalDate allocatedUntil;

	public static RoomAllocation create(Long studentId, Long roomId, LocalDate allocatedFrom) {
		return RoomAllocation.builder()
				.studentId(studentId)
				.roomId(roomId)
				.allocatedFrom(allocatedFrom)
				.build();
	}

	// Ongoing (never vacated) until allocatedUntil is set — mirrors TransportAssignment's
	// effectiveTo-null-means-active convention.
	public boolean isActive() {
		return this.allocatedUntil == null;
	}

	public void vacate(LocalDate allocatedUntil) {
		if (this.allocatedUntil != null) {
			throw new BusinessException(PlatformErrorCode.CONFLICT,
					"Room allocation already vacated on " + this.allocatedUntil);
		}
		if (allocatedUntil.isBefore(this.allocatedFrom)) {
			throw new BusinessException(PlatformErrorCode.INVALID_ARGUMENT,
					"Allocated-until date cannot be before the allocated-from date");
		}
		this.allocatedUntil = allocatedUntil;
	}
}
