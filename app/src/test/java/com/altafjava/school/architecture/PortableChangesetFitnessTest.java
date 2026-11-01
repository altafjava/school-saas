package com.altafjava.school.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** A changeset must run on every supported engine, so raw SQL has no place in one. */
class PortableChangesetFitnessTest {

	private static final Path CHANGESETS = Path.of("../domain/src/main/resources/db");
	private static final Pattern RAW_SQL = Pattern.compile("<(sql|sqlFile|createProcedure|modifySql)\\b");

	@Test
	void changesetsMustUsePortableLiquibaseTagsOnly() throws IOException {
		try (Stream<Path> files = Files.walk(CHANGESETS)) {
			List<Path> changesets = files.filter(file -> file.toString().endsWith(".xml")).toList();

			assertThat(changesets).isNotEmpty().filteredOn(PortableChangesetFitnessTest::containsRawSql).isEmpty();
		}
	}

	private static boolean containsRawSql(Path file) {
		try {
			return RAW_SQL.matcher(Files.readString(file)).find();
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}
}
