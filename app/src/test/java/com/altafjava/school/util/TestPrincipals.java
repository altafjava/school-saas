package com.altafjava.school.util;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.security.Roles;

/**
 * Puts a caller on the test thread for tests that invoke services directly: the services enforce
 * the caller's classroom scope, so a call with no principal is refused.
 */
public final class TestPrincipals {

	private TestPrincipals() {
	}

	public static void authenticateAsTenantAdmin() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				new TestOperator(), null, List.of(new SimpleGrantedAuthority("ROLE_" + Roles.TENANT_ADMIN))));
	}

	public static void clear() {
		SecurityContextHolder.clearContext();
	}

	private record TestOperator() implements AuthenticatedUser {

		@Override
		public Long getId() {
			return -1L;
		}

		@Override
		public String getUsername() {
			return "test-tenant-admin";
		}

		@Override
		public Long getTenantId() {
			return null;
		}
	}
}
