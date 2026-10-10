package com.altafjava.school.application.reference;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;

/**
 * Turns a record's internal id into the public id the API exposes. A record's public id never
 * changes, so answers are cached for the life of the process.
 */
@Component
@RequiredArgsConstructor
public class PublicIdResolver {

	private static final long MAX_CACHED_IDS = 500_000;

	private final JdbcTemplate jdbcTemplate;
	private final Cache<Key, String> cache = Caffeine.newBuilder().maximumSize(MAX_CACHED_IDS).build();

	/** Null for a null id, and for an id that names no record of the current tenant. */
	public String resolve(EntityRef ref, Long id) {
		if (id == null) {
			return null;
		}
		Long tenantId = TenantContext.getCurrentTenantId();
		return cache.get(new Key(ref, tenantId, id), key -> load(ref, tenantId, id));
	}

	// Plain SQL on purpose: a reference to a soft-deleted record must still resolve.
	private String load(EntityRef ref, Long tenantId, Long id) {
		List<String> found = jdbcTemplate.queryForList(
				"SELECT public_id FROM " + ref.table() + " WHERE id = ? AND tenant_id = ?", String.class, id, tenantId);
		return found.isEmpty() ? null : found.get(0);
	}

	private record Key(EntityRef ref, Long tenantId, Long id) {
	}
}
