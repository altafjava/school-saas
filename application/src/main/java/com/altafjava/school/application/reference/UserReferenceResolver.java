package com.altafjava.school.application.reference;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/** Resolves the public id of a platform user, as the API names it, to the user of the current tenant. */
@Component
@RequiredArgsConstructor
public class UserReferenceResolver {

	private final UserRepository userRepository;

	public Long requireUserId(String userPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return userRepository.findByPublicId(parse(userPublicId))
				.filter(user -> tenantId.equals(user.getTenantId()))
				.map(User::getId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userPublicId));
	}

	private UUID parse(String userPublicId) {
		try {
			return UUID.fromString(userPublicId);
		} catch (IllegalArgumentException e) {
			throw new ResourceNotFoundException("User not found: " + userPublicId);
		}
	}
}
