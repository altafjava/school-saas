package com.altafjava.school.domain.timetable.model;

import java.time.DayOfWeek;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "timetable_entries")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TimetableEntry extends SoftDeletableEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false, length = 10)
	private DayOfWeek dayOfWeek;

	// FK to periods.id
	@Column(name = "period_id", nullable = false)
	private Long periodId;

	// FK to classrooms.id
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	// FK to subjects.id
	@Column(name = "subject_id", nullable = false)
	private Long subjectId;

	// FK to teachers.id
	@Column(name = "teacher_id", nullable = false)
	private Long teacherId;

	// FK to venues.id — optional; a slot without a venue takes part in no room-conflict check.
	@Column(name = "venue_id")
	private Long venueId;

	public static TimetableEntry create(DayOfWeek dayOfWeek, Long periodId, Long classroomId, Long subjectId,
			Long teacherId, Long venueId) {
		return TimetableEntry.builder()
				.dayOfWeek(dayOfWeek)
				.periodId(periodId)
				.classroomId(classroomId)
				.subjectId(subjectId)
				.teacherId(teacherId)
				.venueId(venueId)
				.build();
	}

	public void assignVenue(Long venueId) {
		this.venueId = venueId;
	}
}
