package com.altafjava.school.api.dto.request;

import java.util.Map;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateImportMappingRequest(
		@Schema(description = "Target field to file column; a blank column leaves an optional field unmapped") @NotNull @Size(max = 50) Map<String, String> mapping) {

	public UpdateImportMappingRequest {
		mapping = mapping == null ? null : Map.copyOf(mapping);
	}
}
