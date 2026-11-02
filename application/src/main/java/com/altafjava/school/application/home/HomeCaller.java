package com.altafjava.school.application.home;

import java.util.Set;

/** Who is asking, and what their roles let them read; each home card checks its own permission. */
public record HomeCaller(Long userId, Set<String> permissions) {

	public HomeCaller {
		permissions = Set.copyOf(permissions);
	}

	public boolean holds(String permission) {
		return permissions.contains(permission);
	}
}
