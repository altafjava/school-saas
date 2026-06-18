package com.altafjava.school.domain.academicyear.model;

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
@Table(name = "academic_years")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AcademicYear extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "is_current", nullable = false)
	private boolean current;

	public static AcademicYear create(String name, LocalDate startDate, LocalDate endDate, boolean current) {
		return AcademicYear.builder()
				.name(name)
				.startDate(startDate)
				.endDate(endDate)
				.current(current)
				.build();
	}
}
