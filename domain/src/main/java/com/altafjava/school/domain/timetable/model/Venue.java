package com.altafjava.school.domain.timetable.model;

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
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A physical space a timetable slot takes place in, so one room cannot host two slots at once. */
@Entity
@Table(name = "venues")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Venue extends SoftDeletableEntity {

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	// Null means the capacity is not recorded.
	@Column(name = "capacity")
	private Integer capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "venue_type", nullable = false, length = 20)
	private VenueType venueType;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Venue create(String code, String name, VenueType venueType, Integer capacity) {
		requireValidCapacity(capacity);
		return Venue.builder()
				.code(code)
				.name(name)
				.venueType(venueType)
				.capacity(capacity)
				.active(true)
				.build();
	}

	public void updateDetails(String name, VenueType venueType, Integer capacity) {
		requireValidCapacity(capacity);
		this.name = name;
		this.venueType = venueType;
		this.capacity = capacity;
	}

	public void deactivate() {
		this.active = false;
	}

	public void activate() {
		this.active = true;
	}

	private static void requireValidCapacity(Integer capacity) {
		if (capacity != null && capacity <= 0) {
			throw new BusinessException("Venue capacity must be positive");
		}
	}
}
