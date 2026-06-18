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

@Entity
@Table(name = "attendance")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Attendance extends SoftDeletableEntity {

	// FK to students.id — stored as Long to avoid cross-entity coupling in domain layer
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to classrooms.id
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	@Column(name = "attendance_date", nullable = false)
	private LocalDate attendanceDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AttendanceStatus status;

	@Column(name = "marked_by", length = 100)
	private String markedBy;

	public static Attendance create(Long studentId, Long classroomId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
		return Attendance.builder()
				.studentId(studentId)
				.classroomId(classroomId)
				.attendanceDate(attendanceDate)
				.status(status)
				.markedBy(markedBy)
				.build();
	}
}
