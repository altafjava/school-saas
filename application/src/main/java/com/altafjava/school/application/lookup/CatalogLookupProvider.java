package com.altafjava.school.application.lookup;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import com.altafjava.platform.application.lookup.LookupOption;
import com.altafjava.platform.application.lookup.LookupProvider;

/**
 * A lookup over a small, tenant-bounded reference catalog (academic years, departments, ...): the
 * whole catalog is read and filtered by label in memory. Lookups over large entities (students,
 * guardians, ...) have their own provider that searches in the database.
 */
class CatalogLookupProvider<T> implements LookupProvider {

	private final String type;
	private final String requiredPermission;
	private final Supplier<List<T>> catalog;
	private final Function<T, LookupOption> toOption;

	CatalogLookupProvider(String type, String requiredPermission, Supplier<List<T>> catalog,
			Function<T, LookupOption> toOption) {
		this.type = type;
		this.requiredPermission = requiredPermission;
		this.catalog = catalog;
		this.toOption = toOption;
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public String requiredPermission() {
		return requiredPermission;
	}

	@Override
	public List<LookupOption> search(String query, int limit) {
		String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
		return catalog.get().stream()
				.map(toOption)
				.filter(option -> option.label().toLowerCase(Locale.ROOT).contains(needle))
				.limit(limit)
				.toList();
	}
}
