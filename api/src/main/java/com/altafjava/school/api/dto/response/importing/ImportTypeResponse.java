package com.altafjava.school.api.dto.response.importing;

import java.util.List;
import com.altafjava.school.domain.importing.model.ImportType;

public record ImportTypeResponse(ImportType type, List<ImportFieldResponse> fields) {

	public ImportTypeResponse {
		fields = fields == null ? null : List.copyOf(fields);
	}
}
