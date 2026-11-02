package com.altafjava.school.application.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.application.security.ResourceAccessPolicyEnforcer;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.application.policy.ResourceAction;
import com.altafjava.school.application.policy.ResourceType;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

// Enforces "own child/own record only" on student-scoped reads. STUDENT_READ_ALL bypasses it, and
// staff pass for the students they teach; a school extends a role's reach by granting that permission.
@Component
public class StudentDataAccessGuard {

	private final ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer;
	private final PermissionAuthorizationService permissionAuthorizationService;
	private final AcademicAccessGuard academicAccessGuard;
	private final PublicIdLookup publicIdLookup;

	public StudentDataAccessGuard(ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer,
			PermissionAuthorizationService permissionAuthorizationService, AcademicAccessGuard academicAccessGuard,
			PublicIdLookup publicIdLookup) {
		this.resourceAccessPolicyEnforcer = resourceAccessPolicyEnforcer;
		this.permissionAuthorizationService = permissionAuthorizationService;
		this.academicAccessGuard = academicAccessGuard;
		this.publicIdLookup = publicIdLookup;
	}

	public void assertCanView(Long tenantId, String studentPublicId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (permissionAuthorizationService.hasPermission(SchoolPermissions.STUDENT_READ_ALL)
				|| academicAccessGuard.teachesStudent(tenantId, publicIdLookup.idOrNull(EntityRef.STUDENT,
						studentPublicId))) {
			return;
		}
		String userId = resolveUserId(authentication);
		boolean allowed = resourceAccessPolicyEnforcer.isAllowed(userId, tenantId, ResourceType.STUDENT.name(),
				studentPublicId, ResourceAction.READ.name());
		if (!allowed) {
			throw new AccessDeniedException("Not authorized to view student " + studentPublicId + "'s data");
		}
	}

	private String resolveUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return String.valueOf(user.getId());
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot verify student data access");
	}
}
