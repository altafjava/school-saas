package com.altafjava.school.domain.attendance.model;

import java.time.LocalDate;
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

/**
 * Period-level attendance, additive alongside daily {@link Attendance} — not a replacement.
 * Period, subject, and teacher context come from {@code timetableEntryId} (a FK to
 * {@code TimetableEntry}) rather than duplicating those columns here.
 */
@Entity
@Table(name = "period_attendance")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PeriodAttendance extends SoftDeletableEntity {

	// FK to students.id — stored as Long to avoid cross-entity coupling in domain layer
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to classrooms.id
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	// FK to timetable_entries.id — gives period + subject + teacher context via that entry rather
	// than duplicating those columns here.
	@Column(name = "timetable_entry_id", nullable = false)
	private Long timetableEntryId;

	@Column(name = "attendance_date", nullable = false)
	private LocalDate attendanceDate;

	@Column(name = "marked_by", length = 100)
	private String markedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AttendanceStatus status;

	public static PeriodAttendance create(Long studentId, Long classroomId, Long timetableEntryId,
			LocalDate attendanceDate, AttendanceStatus status, String markedBy) {
		return PeriodAttendance.builder()
				.studentId(studentId)
				.classroomId(classroomId)
				.timetableEntryId(timetableEntryId)
				.attendanceDate(attendanceDate)
				.status(status)
				.markedBy(markedBy)
				.build();
	}
}
