package com.altafjava.school.api.dto.response.importing;

import java.time.Instant;
import com.altafjava.school.domain.importing.model.ImportJobStatus;
import com.altafjava.school.domain.importing.model.ImportType;

/** One line of the import history. */
public record ImportJobSummaryResponse(String publicId, ImportType importType, String fileName,
		ImportJobStatus status, int totalRows, Integer validRows, Integer invalidRows, Integer importedRows,
		Integer failedRows, Instant uploadedAt, Instant completedAt) {
}
