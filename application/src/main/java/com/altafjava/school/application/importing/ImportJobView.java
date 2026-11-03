package com.altafjava.school.application.importing;

import java.util.List;
import java.util.Map;
import com.altafjava.school.domain.importing.model.ImportJob;

/** A job with what a mapping screen needs: the file's columns, the fields, the mapping and the first rows. */
public record ImportJobView(ImportJob job, List<String> headers, List<ImportField> fields,
		Map<String, String> mapping, List<Map<String, String>> preview) {

	public ImportJobView {
		headers = List.copyOf(headers);
		fields = List.copyOf(fields);
		mapping = Map.copyOf(mapping);
		preview = List.copyOf(preview);
	}
}
