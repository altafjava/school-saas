package com.altafjava.school.application.importing;

import java.util.List;
import com.altafjava.school.domain.importing.model.ImportType;

/** An import type and the fields its rows carry. */
public record ImportTypeDescription(ImportType type, List<ImportField> fields) {

	public ImportTypeDescription {
		fields = List.copyOf(fields);
	}
}
