package com.altafjava.school.application.home;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.core.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;

/** Resolves the caller's permissions once, so a screen's cost does not grow with its cards. */
@Component
@RequiredArgsConstructor
public class HomeCallerResolver {

	private static final String ROLE_PREFIX = "ROLE_";

	private final PermissionAuthorizationService permissionAuthorizationService;

	public HomeCaller current() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
				|| user.getId() == null || user.getId() < 0) {
			throw new AccessDeniedException("A signed-in user is required");
		}
		List<String> roleNames = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.map(HomeCallerResolver::withoutPrefix)
				.toList();
		return new HomeCaller(user.getId(), permissionAuthorizationService.permissionsOf(roleNames));
	}

	private static String withoutPrefix(String authority) {
		return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
	}
}
