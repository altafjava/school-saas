package com.altafjava.school.demo;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.core.security.AuthenticatedUser;

/** Runs seed work as a given user, because services resolve "the current employee or guardian" from it. */
final class DemoPrincipal implements AuthenticatedUser {

	private final Long id;
	private final Long tenantId;

	private DemoPrincipal(Long id, Long tenantId) {
		this.id = id;
		this.tenantId = tenantId;
	}

	static void runAs(Long userId, Long tenantId, String role, Runnable action) {
		SecurityContext previous = SecurityContextHolder.getContext();
		SecurityContext seeding = SecurityContextHolder.createEmptyContext();
		seeding.setAuthentication(new UsernamePasswordAuthenticationToken(new DemoPrincipal(userId, tenantId), null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role))));
		SecurityContextHolder.setContext(seeding);
		try {
			action.run();
		} finally {
			SecurityContextHolder.setContext(previous);
		}
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getUsername() {
		return "demo-seed";
	}

	@Override
	public Long getTenantId() {
		return tenantId;
	}
}
