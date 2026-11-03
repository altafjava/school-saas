package com.altafjava.school.domain.importing.model;

/**
 * UPLOADED: read and mapped. VALIDATED: a dry run has reported every row. IMPORTING: claimed by one
 * import run. COMPLETED: imported, once.
 */
public enum ImportJobStatus {
	UPLOADED, VALIDATED, IMPORTING, COMPLETED
}
