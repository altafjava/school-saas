package com.altafjava.school.application.importing;

import com.altafjava.platform.core.exception.BusinessException;

/** A row that cannot be imported as it stands; {@code field} names the value at fault, when there is one. */
public class ImportRowException extends BusinessException {

	private final String field;

	public ImportRowException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
