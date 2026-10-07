package com.altafjava.school.domain.reportcard.model;

import java.time.Instant;
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
@Table(name = "report_cards")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ReportCard extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to terms.id
	@Column(name = "term_id", nullable = false)
	private Long termId;

	// FK to platform document_issuances.id — the rendered, verifiable PDF.
	@Column(name = "document_issuance_id", nullable = false)
	private Long documentIssuanceId;

	@Column(name = "generated_at", nullable = false)
	private Instant generatedAt;

	@Column(name = "teacher_remarks", length = 1000)
	private String teacherRemarks;

	@Column(name = "principal_remarks", length = 1000)
	private String principalRemarks;

	public static ReportCard create(Long studentId, Long termId, Long documentIssuanceId) {
		return ReportCard.builder()
				.studentId(studentId)
				.termId(termId)
				.documentIssuanceId(documentIssuanceId)
				.generatedAt(Instant.now())
				.build();
	}

	public void addRemarks(String teacherRemarks, String principalRemarks) {
		this.teacherRemarks = teacherRemarks;
		this.principalRemarks = principalRemarks;
	}
}
