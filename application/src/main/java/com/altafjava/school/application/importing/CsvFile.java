package com.altafjava.school.application.importing;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import com.altafjava.platform.core.exception.BusinessException;

/** An uploaded CSV: its header and data rows, bounded so one upload cannot exhaust the server. */
public record CsvFile(List<String> headers, List<List<String>> rows) {

	public static final int MAX_ROWS = 5000;

	public CsvFile {
		headers = List.copyOf(headers);
		rows = List.copyOf(rows);
	}

	public static CsvFile parse(String content) {
		CSVFormat format = CSVFormat.DEFAULT.builder().setTrim(true).setIgnoreEmptyLines(true).get();
		try (CSVParser parser = CSVParser.builder().setReader(new StringReader(content)).setFormat(format).get()) {
			List<List<String>> records = new ArrayList<>();
			for (CSVRecord record : parser) {
				if (records.size() > MAX_ROWS) {
					throw new BusinessException("A file can hold at most " + MAX_ROWS + " rows");
				}
				records.add(record.toList());
			}
			if (records.isEmpty()) {
				throw new BusinessException("The file is empty; the first line must name the columns");
			}
			return new CsvFile(records.getFirst(), records.subList(1, records.size()));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read the CSV file", e);
		} catch (IllegalStateException | java.io.UncheckedIOException e) {
			throw new BusinessException("The file is not valid CSV: " + e.getMessage());
		}
	}

	/** The value under a column in a data row; a short row has nothing there. */
	public String value(List<String> row, String header) {
		int index = headers.indexOf(header);
		return index >= 0 && index < row.size() ? row.get(index) : null;
	}
}
