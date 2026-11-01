package com.altafjava.school.application.bulk;

import java.util.List;

/** Per-row outcome of a roster entry; a failed row never blocks the others. */
public record BulkEntryResult(int succeeded, int failed, List<Row> rows) {

	public BulkEntryResult {
		rows = List.copyOf(rows);
	}

	public enum Outcome {
		CREATED, UPDATED, UNCHANGED, FAILED
	}

	public record Row(String studentPublicId, Outcome outcome, String recordPublicId, String error) {

		public static Row written(String studentPublicId, Outcome outcome, String recordPublicId) {
			return new Row(studentPublicId, outcome, recordPublicId, null);
		}

		public static Row failed(String studentPublicId, String error) {
			return new Row(studentPublicId, Outcome.FAILED, null, error);
		}
	}

	public static BulkEntryResult of(List<Row> rows) {
		int failed = (int) rows.stream().filter(row -> row.outcome() == Outcome.FAILED).count();
		return new BulkEntryResult(rows.size() - failed, failed, rows);
	}
}
