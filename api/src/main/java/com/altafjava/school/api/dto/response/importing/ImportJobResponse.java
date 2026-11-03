package com.altafjava.school.api.dto.response.importing;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.altafjava.school.domain.importing.model.ImportJobStatus;
import com.altafjava.school.domain.importing.model.ImportType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One uploaded file: its columns, the field-to-column mapping, the first rows, and what the dry "
		+ "run and the import made of it. Line numbers count the header as line 1.")
public record ImportJobResponse(String publicId, Long version, ImportType importType, String fileName,
		ImportJobStatus status, int totalRows, Integer validRows, Integer invalidRows, Integer importedRows,
		Integer failedRows, Instant uploadedAt, Instant validatedAt, Instant completedAt, List<String> headers,
		List<ImportFieldResponse> fields,
		@Schema(description = "Target field to the file column it reads") Map<String, String> mapping,
		@Schema(description = "Up to the first ten rows, by column") List<Map<String, String>> preview) {

	public ImportJobResponse {
		headers = headers == null ? null : List.copyOf(headers);
		fields = fields == null ? null : List.copyOf(fields);
		mapping = mapping == null ? null : Map.copyOf(mapping);
		preview = preview == null ? null : List.copyOf(preview);
	}
}
