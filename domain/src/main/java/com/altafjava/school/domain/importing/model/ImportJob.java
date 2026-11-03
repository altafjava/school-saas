package com.altafjava.school.domain.importing.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One uploaded file and what became of it: the column mapping chosen, the dry run's verdict and the
 * import's outcome. It is the import history, and it can be imported only once.
 */
@Entity
@Table(name = "import_jobs")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class ImportJob extends SoftDeletableEntity {

	// FK to platform users.id
	@Column(name = "uploaded_by_user_id")
	private Long uploadedByUserId;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	// The file as uploaded, kept so a dry run and the import read exactly the same rows.
	@Column(name = "content", nullable = false)
	private String content;

	// Target field to source column, as JSON.
	@Column(name = "column_mapping", nullable = false, length = 4000)
	private String columnMapping;

	@Column(name = "total_rows", nullable = false)
	private int totalRows;

	@Column(name = "valid_rows")
	private Integer validRows;

	@Column(name = "invalid_rows")
	private Integer invalidRows;

	@Column(name = "imported_rows")
	private Integer importedRows;

	@Column(name = "failed_rows")
	private Integer failedRows;

	@Column(name = "validated_at")
	private Instant validatedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "import_type", nullable = false, length = 30)
	private ImportType importType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ImportJobStatus status;

	public static ImportJob upload(ImportType importType, String fileName, String content, int totalRows,
			String columnMapping, Long uploadedByUserId) {
		return ImportJob.builder()
				.importType(importType)
				.fileName(fileName)
				.content(content)
				.totalRows(totalRows)
				.columnMapping(columnMapping)
				.uploadedByUserId(uploadedByUserId)
				.status(ImportJobStatus.UPLOADED)
				.build();
	}

	/** A new mapping makes any earlier dry run meaningless. */
	public void remap(String columnMapping) {
		requireNotCompleted();
		this.columnMapping = columnMapping;
		this.status = ImportJobStatus.UPLOADED;
		this.validRows = null;
		this.invalidRows = null;
		this.validatedAt = null;
	}

	public void recordDryRun(int valid, int invalid) {
		requireNotCompleted();
		this.validRows = valid;
		this.invalidRows = invalid;
		this.validatedAt = Instant.now();
		this.status = ImportJobStatus.VALIDATED;
	}

	/** Claims the job for one run; saved before any row is written, so a second run fails on the version. */
	public void beginImport() {
		requireNotCompleted();
		this.status = ImportJobStatus.IMPORTING;
	}

	public void recordImport(int imported, int failed) {
		if (status != ImportJobStatus.IMPORTING) {
			throw new BusinessException("The import was not started");
		}
		this.importedRows = imported;
		this.failedRows = failed;
		this.completedAt = Instant.now();
		this.status = ImportJobStatus.COMPLETED;
	}

	public void requireNotCompleted() {
		if (status == ImportJobStatus.COMPLETED || status == ImportJobStatus.IMPORTING) {
			throw new BusinessException("This file has already been imported; upload it again to import more");
		}
	}
}
