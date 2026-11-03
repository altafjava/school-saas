package com.altafjava.school.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * School dates and times are the school's wall clock, which can be most of a day away from the
 * server's. Only {@code SchoolClock} may read the current time; everything else asks it.
 */
class SchoolClockFitnessTest {

	private static final Path REPOSITORY_ROOT = Path.of("..");
	private static final Pattern SERVER_CLOCK = Pattern
			.compile("\\b(LocalDate|LocalDateTime|LocalTime|YearMonth|Year|ZonedDateTime|OffsetDateTime)\\.now\\(");

	@Test
	void productionCode_readsTheTimeOnlyThroughSchoolClock() throws IOException {
		List<String> findings = new ArrayList<>();
		for (Path source : mainSources()) {
			if (source.getFileName().toString().equals("SchoolClock.java")) {
				continue;
			}
			List<String> lines = Files.readAllLines(source);
			for (int index = 0; index < lines.size(); index++) {
				if (SERVER_CLOCK.matcher(lines.get(index)).find()) {
					findings.add(source.getFileName() + ":" + (index + 1) + " " + lines.get(index).trim());
				}
			}
		}
		assertThat(findings).isEmpty();
	}

	private static List<Path> mainSources() throws IOException {
		try (Stream<Path> files = Files.walk(REPOSITORY_ROOT)) {
			return files.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> path.toString().contains("/src/main/java/"))
					.toList();
		}
	}
}
