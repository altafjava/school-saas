package com.altafjava.school.domain.student.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A family of students at the school. It carries no data of its own: membership is the
 * {@code Student#siblingGroupId} of each member, so merging two families is re-pointing members
 * rather than copying pairs.
 */
@Entity
@Table(name = "sibling_groups")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class SiblingGroup extends SoftDeletableEntity {

	public static SiblingGroup create() {
		return SiblingGroup.builder().build();
	}
}
