package com.altafjava.school.domain.importing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Why one row of a file was not, or would not be, imported. */
@Entity
@Table(name = "import_job_errors")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class ImportJobError extends SoftDeletableEntity {

	// FK to import_jobs.id
	@Column(name = "import_job_id", nullable = false)
	private Long importJobId;

	// The line in the file; the header is line 1.
	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	// The target field at fault, when the error is about one value.
	@Column(name = "field", length = 100)
	private String field;

	@Column(name = "message", nullable = false, length = 1000)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(name = "phase", nullable = false, length = 20)
	private ImportPhase phase;

	public static ImportJobError of(Long importJobId, ImportPhase phase, int lineNumber, String field, String message) {
		String bounded = message == null ? "Unknown error"
				: message.length() > 1000 ? message.substring(0, 1000)
						: message;
		return ImportJobError.builder().importJobId(importJobId).phase(phase).lineNumber(lineNumber).field(field)
				.message(bounded).build();
	}
}
