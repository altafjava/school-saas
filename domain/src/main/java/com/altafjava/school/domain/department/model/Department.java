package com.altafjava.school.domain.department.model;

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
@Table(name = "departments")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Department extends SoftDeletableEntity {

	// FK to employees.id — nullable, a department need not have a head assigned yet.
	@Column(name = "head_employee_id")
	private Long headEmployeeId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Department create(String name, String code, String description) {
		return Department.builder()
				.name(name)
				.code(code)
				.description(description)
				.active(true)
				.build();
	}

	public void updateDetails(String name, String code, String description) {
		this.name = name;
		this.code = code;
		this.description = description;
	}

	public void assignHeadEmployee(Long headEmployeeId) {
		this.headEmployeeId = headEmployeeId;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
