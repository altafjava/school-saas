package com.altafjava.school.domain.timetable.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One teacher covering one regular timetable slot on one date. The regular timetable is untouched;
 * this is the dated exception to it.
 */
@Entity
@Table(name = "timetable_substitutions")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class TimetableSubstitution extends SoftDeletableEntity {

	// FK to timetable_entries.id
	@Column(name = "timetable_entry_id", nullable = false)
	private Long timetableEntryId;

	// FK to periods.id — copied from the entry so the database can stop a teacher covering two
	// slots of the same period on one date.
	@Column(name = "period_id", nullable = false)
	private Long periodId;

	@Column(name = "substitution_date", nullable = false)
	private LocalDate substitutionDate;

	// FK to teachers.id
	@Column(name = "substitute_teacher_id", nullable = false)
	private Long substituteTeacherId;

	@Column(name = "reason", length = 500)
	private String reason;

	// FK to platform users.id
	@Column(name = "assigned_by_user_id")
	private Long assignedByUserId;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "cancellation_reason", length = 500)
	private String cancellationReason;

	public static TimetableSubstitution assign(TimetableEntry entry, LocalDate substitutionDate,
			Long substituteTeacherId, String reason, Long assignedByUserId, LocalDate today) {
		DayOfWeek day = entry.getDayOfWeek();
		if (substitutionDate.isBefore(today)) {
			throw new BusinessException("A substitute cannot be assigned for a date that has passed");
		}
		if (substitutionDate.getDayOfWeek() != day) {
			throw new BusinessException("The slot takes place on " + day + ", but " + substitutionDate + " is a "
					+ substitutionDate.getDayOfWeek());
		}
		if (entry.getTeacherId().equals(substituteTeacherId)) {
			throw new BusinessException("The substitute must be someone other than the regular teacher");
		}
		return TimetableSubstitution.builder()
				.timetableEntryId(entry.getId())
				.periodId(entry.getPeriodId())
				.substitutionDate(substitutionDate)
				.substituteTeacherId(substituteTeacherId)
				.reason(reason)
				.assignedByUserId(assignedByUserId)
				.build();
	}

	public boolean isActive() {
		return cancelledAt == null;
	}

	public void cancel(String reason, LocalDate today) {
		if (!isActive()) {
			throw new BusinessException("The substitution is already cancelled");
		}
		if (substitutionDate.isBefore(today)) {
			throw new BusinessException("A substitution that has already taken place cannot be cancelled");
		}
		this.cancelledAt = Instant.now();
		this.cancellationReason = reason;
	}
}
