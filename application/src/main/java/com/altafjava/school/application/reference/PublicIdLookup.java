package com.altafjava.school.application.reference;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;

/**
 * Turns the public id a list filter carries into the internal id its query compares against; the
 * counterpart of {@link PublicIdResolver}. A record's id and public id never change, so answers are
 * cached for the life of the process.
 */
@Component
@RequiredArgsConstructor
public class PublicIdLookup {

	private static final long MAX_CACHED_IDS = 500_000;

	private final JdbcTemplate jdbcTemplate;
	private final Cache<Key, Long> cache = Caffeine.newBuilder().maximumSize(MAX_CACHED_IDS).build();

	/**
	 * The internal id of the current tenant's record, or {@code null} when no filter was given.
	 *
	 * @throws IllegalArgumentException
	 *                                       the public id is not a UUID
	 * @throws ResourceNotFoundException
	 *                                       no record of the current tenant has that public id
	 */
	public Long idOrNull(EntityRef ref, String publicId) {
		if (publicId == null || publicId.isBlank()) {
			return null;
		}
		UUID parsed = UUID.fromString(publicId.strip());
		Long tenantId = TenantContext.getCurrentTenantId();
		Long id = cache.get(new Key(ref, tenantId, parsed), key -> load(ref, tenantId, parsed));
		if (id == null) {
			throw new ResourceNotFoundException(ref.label() + " not found: " + publicId);
		}
		return id;
	}

	// Plain SQL on purpose: filtering by a soft-deleted record must keep working for history views.
	private Long load(EntityRef ref, Long tenantId, UUID publicId) {
		List<Long> found = jdbcTemplate.queryForList(
				"SELECT id FROM " + ref.table() + " WHERE public_id = ? AND tenant_id = ?", Long.class,
				publicId.toString(), tenantId);
		return found.isEmpty() ? null : found.get(0);
	}

	private record Key(EntityRef ref, Long tenantId, UUID publicId) {
	}
}
