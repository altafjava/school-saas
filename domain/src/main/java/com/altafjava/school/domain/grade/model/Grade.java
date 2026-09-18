package com.altafjava.school.domain.grade.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "grades")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Grade extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to subjects.id
	@Column(name = "subject_id", nullable = false)
	private Long subjectId;

	// FK to exams.id
	@Column(name = "exam_id", nullable = false)
	private Long examId;

	@Column(name = "marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal marks;

	@Column(name = "grade_letter", nullable = false, length = 5)
	private String gradeLetter;

	@Column(name = "graded_by", length = 100)
	private String gradedBy;

	public static Grade create(Long studentId, Long subjectId, Long examId,
			BigDecimal marks, String gradeLetter, String gradedBy) {
		return Grade.builder()
				.studentId(studentId)
				.subjectId(subjectId)
				.examId(examId)
				.marks(marks)
				.gradeLetter(gradeLetter)
				.gradedBy(gradedBy)
				.build();
	}

	// Callers must record a GradeCorrection with the pre-correction marks/gradeLetter before
	// calling this — see GradeService#correct — so the change is reconstructable from history,
	// not just visible via BaseEntity's updatedBy/updatedAt.
	public void correct(BigDecimal marks, String gradeLetter) {
		this.marks = marks;
		this.gradeLetter = gradeLetter;
	}
}
