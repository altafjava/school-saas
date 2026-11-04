package com.altafjava.school.domain.transport.model;

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
@Table(name = "transport_assignments")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TransportAssignment extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "route_id", nullable = false)
	private Long routeId;

	@Column(name = "vehicle_id", nullable = false)
	private Long vehicleId;

	@Column(name = "route_stop_id", nullable = false)
	private Long routeStopId;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	private LocalDate effectiveTo;

	public static TransportAssignment create(Long studentId, Long routeId, Long vehicleId, Long routeStopId,
			LocalDate effectiveFrom) {
		return TransportAssignment.builder()
				.studentId(studentId)
				.routeId(routeId)
				.vehicleId(vehicleId)
				.routeStopId(routeStopId)
				.effectiveFrom(effectiveFrom)
				.build();
	}

	public void end(LocalDate effectiveTo) {
		if (this.effectiveTo != null) {
			throw new BusinessException(PlatformErrorCode.CONFLICT,
					"Transport assignment already ended on " + this.effectiveTo);
		}
		if (effectiveTo.isBefore(this.effectiveFrom)) {
			throw new BusinessException(PlatformErrorCode.INVALID_ARGUMENT,
					"Effective-to date cannot be before the effective-from date");
		}
		this.effectiveTo = effectiveTo;
	}
}
