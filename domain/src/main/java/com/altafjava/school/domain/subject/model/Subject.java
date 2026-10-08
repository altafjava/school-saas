package com.altafjava.school.domain.subject.model;

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
@Table(name = "subjects")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Subject extends SoftDeletableEntity {

	// FK to curricula.id — stored as Long to avoid cross-entity coupling in domain layer, same
	// convention as Classroom.curriculumId. Nullable: not every subject is tied to a curriculum.
	@Column(name = "curriculum_id")
	private Long curriculumId;

	@Column(name = "code", nullable = false, length = 20)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Subject create(String code, String name, String description) {
		return Subject.builder()
				.code(code)
				.name(name)
				.description(description)
				.active(true)
				.build();
	}

	public void deactivate() {
		this.active = false;
	}

	public void assignCurriculum(Long curriculumId) {
		this.curriculumId = curriculumId;
	}
}
