package com.altafjava.school.application.importing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/** One data row, keyed by target field after the column mapping. Readers name the field in any error. */
public record ImportRow(int lineNumber, Map<String, String> values) {

	public ImportRow {
		values = Map.copyOf(values);
	}

	/** Absent for a blank value. */
	public String text(String field) {
		String value = values.get(field);
		return value == null || value.isBlank() ? null : value.strip();
	}

	public String requiredText(String field) {
		String value = text(field);
		if (value == null) {
			throw new ImportRowException(field, field + " is required");
		}
		return value;
	}

	public LocalDate date(String field) {
		String value = text(field);
		try {
			return value == null ? null : LocalDate.parse(value);
		} catch (DateTimeParseException e) {
			throw new ImportRowException(field, field + " must be a date like 2026-04-30, was " + value);
		}
	}

	public LocalDate requiredDate(String field) {
		requiredText(field);
		return date(field);
	}

	public BigDecimal requiredAmount(String field) {
		String value = requiredText(field);
		try {
			BigDecimal amount = new BigDecimal(value);
			if (amount.signum() <= 0 || amount.scale() > 2) {
				throw new ImportRowException(field, field + " must be a positive amount with at most 2 decimals");
			}
			return amount;
		} catch (NumberFormatException e) {
			throw new ImportRowException(field, field + " must be a number, was " + value);
		}
	}

	public boolean flag(String field) {
		String value = text(field);
		if (value == null) {
			return false;
		}
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "true", "yes", "y", "1" -> true;
			case "false", "no", "n", "0" -> false;
			default -> throw new ImportRowException(field, field + " must be yes or no, was " + value);
		};
	}

	public <E extends Enum<E>> E choice(String field, Class<E> type, E fallback) {
		String value = text(field);
		if (value == null) {
			return fallback;
		}
		try {
			return Enum.valueOf(type, value.toUpperCase(Locale.ROOT).replace(' ', '_'));
		} catch (IllegalArgumentException e) {
			throw new ImportRowException(field, field + " must be one of " + Arrays.toString(type.getEnumConstants())
					+ ", was " + value);
		}
	}

	public <E extends Enum<E>> E requiredChoice(String field, Class<E> type) {
		requiredText(field);
		return choice(field, type, null);
	}
}
