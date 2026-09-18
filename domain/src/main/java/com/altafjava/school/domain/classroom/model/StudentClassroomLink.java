package com.altafjava.school.domain.classroom.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "student_classroom_links")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class StudentClassroomLink extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to classrooms.id
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	// FK to academic_years.id — a student holds only one active classroom per academic year.
	@Column(name = "academic_year_id", nullable = false)
	private Long academicYearId;

	@Column(name = "enrolled_at", nullable = false)
	private LocalDate enrolledAt;

	public static StudentClassroomLink create(Long studentId, Long classroomId, Long academicYearId,
			LocalDate enrolledAt) {
		return StudentClassroomLink.builder()
				.studentId(studentId)
				.classroomId(classroomId)
				.academicYearId(academicYearId)
				.enrolledAt(enrolledAt)
				.build();
	}
}
