package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.application.security.ResourceAccessPolicyEnforcer;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;

@ExtendWith(MockitoExtension.class)
class StudentDataAccessGuardTest {

	@Mock
	private ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer;
	@Mock
	private PermissionAuthorizationService permissionAuthorizationService;

	@Mock
	private AcademicAccessGuard academicAccessGuard;
	@Mock
	private PublicIdLookup publicIdLookup;

	private StudentDataAccessGuard guard;

	@BeforeEach
	void setUp() {
		guard = new StudentDataAccessGuard(resourceAccessPolicyEnforcer, permissionAuthorizationService,
				academicAccessGuard, publicIdLookup);
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void assertCanView_holdingStudentReadAllPermission_bypassesOwnershipCheck() {
		authenticateAs(1L, "ROLE_TENANT_ADMIN");
		when(permissionAuthorizationService.hasPermission("STUDENT_READ_ALL")).thenReturn(true);

		assertDoesNotThrow(() -> guard.assertCanView(1L, "student-public-id"));
	}

	@Test
	void assertCanView_asTeacherOfTheStudent_allowed() {
		authenticateAs(7L, "ROLE_TEACHER");
		when(publicIdLookup.idOrNull(EntityRef.STUDENT, "student-public-id")).thenReturn(3L);
		when(academicAccessGuard.teachesStudent(1L, 3L)).thenReturn(true);

		assertDoesNotThrow(() -> guard.assertCanView(1L, "student-public-id"));
	}

	@Test
	void assertCanView_asParentOfLinkedStudent_allowed() {
		authenticateAs(5L, "ROLE_PARENT");
		when(permissionAuthorizationService.hasPermission("STUDENT_READ_ALL")).thenReturn(false);
		when(resourceAccessPolicyEnforcer.isAllowed("5", 1L, "STUDENT", "student-public-id", "READ"))
				.thenReturn(true);

		assertDoesNotThrow(() -> guard.assertCanView(1L, "student-public-id"));
	}

	@Test
	void assertCanView_asParentOfAnotherStudent_throwsAccessDenied() {
		authenticateAs(5L, "ROLE_PARENT");
		when(permissionAuthorizationService.hasPermission("STUDENT_READ_ALL")).thenReturn(false);
		when(resourceAccessPolicyEnforcer.isAllowed(any(), any(), any(), any(), any())).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> guard.assertCanView(1L, "student-public-id"));
	}

	private void authenticateAs(Long userId, String authority) {
		AuthenticatedUser principal = new AuthenticatedUser() {
			@Override
			public Long getId() {
				return userId;
			}

			@Override
			public String getUsername() {
				return "user-" + userId;
			}

			@Override
			public Long getTenantId() {
				return 1L;
			}
		};
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}
}
