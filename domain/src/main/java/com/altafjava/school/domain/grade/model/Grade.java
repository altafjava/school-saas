package com.altafjava.school.domain.grade.model;

import java.math.BigDecimal;
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
@Table(name = "grades")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Grade extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "subject", nullable = false, length = 100)
	private String subject;

	// FK to exams.id
	@Column(name = "exam_id", nullable = false)
	private Long examId;

	@Column(name = "marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal marks;

	@Column(name = "grade_letter", nullable = false, length = 5)
	private String gradeLetter;

	@Column(name = "graded_by", length = 100)
	private String gradedBy;

	public static Grade create(Long studentId, String subject, Long examId,
			BigDecimal marks, String gradeLetter, String gradedBy) {
		return Grade.builder()
				.studentId(studentId)
				.subject(subject)
				.examId(examId)
				.marks(marks)
				.gradeLetter(gradeLetter)
				.gradedBy(gradedBy)
				.build();
	}
}
