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
 * Production code must not pretend. A feature is real, removed, or explicitly disabled, so stand-in
 * classes and comments admitting to one fail the build.
 */
class ProductionCodeHonestyFitnessTest {

	private static final Path REPOSITORY_ROOT = Path.of("..");
	private static final Pattern STAND_IN_CLASS = Pattern.compile("(Mock|Fake|Dummy|Stub|NoOp)[A-Z].*\\.java");
	private static final Pattern ADMISSION = Pattern.compile(
			"(?i)(//|^\\s*\\*).*(\\bmock\\b|\\bdummy\\b|\\bsimulat|for now|for simplicity|in a real (app|system|scenario|implementation)|\\bTODO\\b)");

	@Test
	void productionCode_hasNoStandInClasses() throws IOException {
		assertThat(mainSources().stream().map(path -> path.getFileName().toString()))
				.noneMatch(name -> STAND_IN_CLASS.matcher(name).matches());
	}

	@Test
	void productionCode_admitsToNoShortcuts() throws IOException {
		List<String> findings = new ArrayList<>();
		for (Path source : mainSources()) {
			List<String> lines = Files.readAllLines(source);
			for (int index = 0; index < lines.size(); index++) {
				if (ADMISSION.matcher(lines.get(index)).find()) {
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
