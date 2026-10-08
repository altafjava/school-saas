package com.altafjava.school.domain.curriculum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A tenant-defined curriculum under a {@link Board} (e.g. "CBSE — Grades 1-5"), optionally carrying
 * its own {@link GradingScale}. Classrooms attach to a curriculum (see {@code Classroom}); when a
 * classroom's curriculum has no grading scale assigned, grading falls back to the tenant's default
 * scale — see {@code GradingScaleService.resolveEffectiveThresholds}.
 */
@Entity
@Table(name = "curricula")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Curriculum extends SoftDeletableEntity {

	@Column(name = "board_id", nullable = false)
	private Long boardId;

	@Column(name = "grading_scale_id")
	private Long gradingScaleId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Curriculum create(Long boardId, String name, String code, String description) {
		return Curriculum.builder()
				.boardId(boardId)
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

	public void assignGradingScale(Long gradingScaleId) {
		this.gradingScaleId = gradingScaleId;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
