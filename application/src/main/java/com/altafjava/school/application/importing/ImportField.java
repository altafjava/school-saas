package com.altafjava.school.application.importing;

/** A value an import type reads from each row. */
public record ImportField(String name, boolean required, String description) {

	public static ImportField required(String name, String description) {
		return new ImportField(name, true, description);
	}

	public static ImportField optional(String name, String description) {
		return new ImportField(name, false, description);
	}
}
