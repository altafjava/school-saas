package com.altafjava.school.domain.term.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "terms")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Term extends SoftDeletableEntity {

	// FK to academic_years.id — stored as Long to avoid cross-entity coupling in domain layer
	@Column(name = "academic_year_id", nullable = false)
	private Long academicYearId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "is_current", nullable = false)
	private boolean current;

	public static Term create(String name, LocalDate startDate, LocalDate endDate, Long academicYearId) {
		return Term.builder()
				.name(name)
				.startDate(startDate)
				.endDate(endDate)
				.academicYearId(academicYearId)
				.current(false)
				.build();
	}

	public void markCurrent() {
		this.current = true;
	}

	public void markNotCurrent() {
		this.current = false;
	}
}
