package com.altafjava.school.application.importing;

import java.util.Map;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.domain.error.SchoolErrorCode;

/** A row that cannot be imported as it stands; {@code field} names the value at fault, when there is one. */
public class ImportRowException extends BusinessException {

	private final String field;

	public ImportRowException(String field, String message) {
		super(SchoolErrorCode.IMPORT_ROW_INVALID, message, field == null ? Map.of() : Map.of("field", field));
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
