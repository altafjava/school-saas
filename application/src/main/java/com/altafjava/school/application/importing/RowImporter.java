package com.altafjava.school.application.importing;

import java.util.List;
import java.util.Optional;
import com.altafjava.school.domain.importing.model.ImportType;

/**
 * Brings one kind of record in from a row. {@link #apply} validates before it writes, and goes
 * through the same services a single manual entry uses, so an import obeys every rule they do.
 */
public interface RowImporter {

	ImportType type();

	/** The permission a caller needs to bring this kind of record in, beyond being allowed to import. */
	String requiredPermission();

	List<ImportField> fields();

	/** What makes a row unique within one file, so two rows for the same record are caught before writing. */
	Optional<String> uniqueKey(ImportRow row);

	void apply(ImportRow row);
}
