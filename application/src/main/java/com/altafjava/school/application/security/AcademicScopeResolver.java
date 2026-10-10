package com.altafjava.school.application.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

/**
 * Builds the caller's {@link AcademicScope} from permissions and their own school records, never
 * from role names, so a tenant-defined role is scoped exactly like a seeded one.
 */
@Component
@RequiredArgsConstructor
public class AcademicScopeResolver {

	private final PermissionAuthorizationService permissionAuthorizationService;
	private final TeachingAssignmentResolver teachingAssignmentResolver;
	private final OwnStudentResolver ownStudentResolver;

	public AcademicScope current(Long tenantId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			return AcademicScope.NONE;
		}
		boolean writesAll = permissionAuthorizationService.hasPermission(SchoolPermissions.ALL_CLASSROOMS_WRITE);
		boolean readsAll = writesAll
				|| permissionAuthorizationService.hasPermission(SchoolPermissions.ALL_CLASSROOMS_READ);
		return new AcademicScope(readsAll, writesAll, teachingAssignmentResolver.forUser(user.getId(), tenantId),
				ownStudentResolver.forUser(user.getId(), tenantId));
	}
}
