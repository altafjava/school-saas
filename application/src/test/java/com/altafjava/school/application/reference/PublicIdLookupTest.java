package com.altafjava.school.application.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;

class PublicIdLookupTest {

	private static final Long TENANT_ID = 7L;

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final PublicIdLookup lookup = new PublicIdLookup(jdbcTemplate);

	@BeforeEach
	void setUp() {
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, "t", TenantType.SHARED);
	}

	@AfterEach
	void tearDown() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void idOrNull_returnsNull_whenNoFilterWasGiven() {
		assertNull(lookup.idOrNull(EntityRef.CLASSROOM, null));
		assertNull(lookup.idOrNull(EntityRef.CLASSROOM, "  "));

		verifyNoInteractions(jdbcTemplate);
	}

	@Test
	void idOrNull_resolvesTheInternalIdWithinTheCurrentTenant() {
		UUID publicId = UUID.randomUUID();
		when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(publicId.toString()), eq(TENANT_ID)))
				.thenReturn(List.of(42L));

		assertEquals(42L, lookup.idOrNull(EntityRef.CLASSROOM, publicId.toString()));
	}

	@Test
	void idOrNull_throwsNotFound_whenTheRecordBelongsToNoOneInThisTenant() {
		UUID publicId = UUID.randomUUID();
		when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any())).thenReturn(List.of());

		ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class,
				() -> lookup.idOrNull(EntityRef.STUDENT, publicId.toString()));

		assertEquals("Student not found: " + publicId, thrown.getMessage());
	}

	@Test
	void idOrNull_rejectsAPublicIdThatIsNotAUuid() {
		assertThrows(IllegalArgumentException.class, () -> lookup.idOrNull(EntityRef.STUDENT, "not-a-uuid"));

		verify(jdbcTemplate, never()).queryForList(anyString(), eq(Long.class), any(), any());
	}

	@Test
	void idOrNull_cachesAFoundId() {
		UUID publicId = UUID.randomUUID();
		when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any())).thenReturn(List.of(5L));

		lookup.idOrNull(EntityRef.TERM, publicId.toString());
		lookup.idOrNull(EntityRef.TERM, publicId.toString());

		verify(jdbcTemplate, times(1)).queryForList(anyString(), eq(Long.class), any(), any());
	}
}
