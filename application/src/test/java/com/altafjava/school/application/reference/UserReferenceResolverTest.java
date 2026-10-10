package com.altafjava.school.application.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserReferenceResolverTest {

	private static final UUID USER_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private UserRepository userRepository;
	@InjectMocks
	private UserReferenceResolver resolver;

	@BeforeEach
	void setUp() {
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void requireUserId_userOfTheCurrentTenant_returnsInternalId() {
		stubUser(42L, 1L);

		assertEquals(42L, resolver.requireUserId(USER_PUBLIC_ID.toString()));
	}

	@Test
	void requireUserId_userOfAnotherTenant_isNotFound() {
		stubUser(42L, 2L);

		assertThrows(ResourceNotFoundException.class, () -> resolver.requireUserId(USER_PUBLIC_ID.toString()));
	}

	@Test
	void requireUserId_unknownOrMalformedId_isNotFound() {
		when(userRepository.findByPublicId(USER_PUBLIC_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> resolver.requireUserId(USER_PUBLIC_ID.toString()));
		assertThrows(ResourceNotFoundException.class, () -> resolver.requireUserId("not-a-uuid"));
	}

	private void stubUser(Long id, Long tenantId) {
		User user = mock(User.class);
		lenient().when(user.getId()).thenReturn(id);
		when(user.getTenantId()).thenReturn(tenantId);
		when(userRepository.findByPublicId(USER_PUBLIC_ID)).thenReturn(Optional.of(user));
	}
}
