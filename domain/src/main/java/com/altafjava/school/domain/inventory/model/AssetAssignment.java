package com.altafjava.school.domain.inventory.model;

import java.time.LocalDate;
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

@Entity
@Table(name = "asset_assignments")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AssetAssignment extends SoftDeletableEntity {

	@Column(name = "asset_id", nullable = false)
	private Long assetId;

	// Either a teachers.id (STAFF) or a classrooms.id (CLASSROOM), per assignedToType — not a
	// single FK since it targets two different tables.
	@Column(name = "assigned_to_id", nullable = false)
	private Long assignedToId;

	@Column(name = "assigned_at", nullable = false)
	private LocalDate assignedAt;

	@Column(name = "returned_at")
	private LocalDate returnedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "assigned_to_type", nullable = false, length = 20)
	private AssignedToType assignedToType;

	public static AssetAssignment create(Long assetId, AssignedToType assignedToType, Long assignedToId,
			LocalDate assignedAt) {
		return AssetAssignment.builder()
				.assetId(assetId)
				.assignedToType(assignedToType)
				.assignedToId(assignedToId)
				.assignedAt(assignedAt)
				.build();
	}

	public void markReturned(LocalDate returnedAt) {
		if (this.returnedAt != null) {
			throw new BusinessException("Asset assignment already returned on " + this.returnedAt);
		}
		this.returnedAt = returnedAt;
	}
}
