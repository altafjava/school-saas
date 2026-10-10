package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@ExtendWith(MockitoExtension.class)
class AcademicScopeResolverTest {

	private static final Long TENANT_ID = 1L;
	private static final Long USER_ID = 7L;

	@Mock
	private PermissionAuthorizationService permissionAuthorizationService;
	@Mock
	private TeachingAssignmentResolver teachingAssignmentResolver;
	@Mock
	private OwnStudentResolver ownStudentResolver;
	@InjectMocks
	private AcademicScopeResolver resolver;

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void current_withoutAuthenticatedUser_isEmptyScope() {
		assertSame(AcademicScope.NONE, resolver.current(TENANT_ID));
	}

	@Test
	void current_withWriteAllPermission_readsAndWritesEveryClassroom() {
		authenticate();
		when(permissionAuthorizationService.hasPermission(SchoolPermissions.ALL_CLASSROOMS_WRITE)).thenReturn(true);
		stubRecords(TeachingAssignments.NONE, Set.of());

		AcademicScope scope = resolver.current(TENANT_ID);

		assertTrue(scope.readsAllClassrooms());
		assertTrue(scope.canWriteSubject(99L, 100L));
	}

	@Test
	void current_withReadAllPermissionOnly_readsEveryClassroomButWritesNone() {
		authenticate();
		when(permissionAuthorizationService.hasPermission(SchoolPermissions.ALL_CLASSROOMS_WRITE)).thenReturn(false);
		when(permissionAuthorizationService.hasPermission(SchoolPermissions.ALL_CLASSROOMS_READ)).thenReturn(true);
		stubRecords(TeachingAssignments.NONE, Set.of());

		AcademicScope scope = resolver.current(TENANT_ID);

		assertTrue(scope.canReadClassroom(99L));
		assertFalse(scope.canWriteClassroom(99L));
	}

	@Test
	void current_withoutScopePermissions_reachesOnlyTaughtClassroomsAndOwnStudents() {
		authenticate();
		TeachingAssignments teaching = new TeachingAssignments(70L, Set.of(10L), Map.of(20L, Set.of(100L)));
		stubRecords(teaching, Set.of(50L));

		AcademicScope scope = resolver.current(TENANT_ID);

		assertFalse(scope.readsAllClassrooms());
		assertTrue(scope.canWriteClassroom(10L));
		assertTrue(scope.canWriteSubject(20L, 100L));
		assertFalse(scope.canWriteSubject(20L, 200L));
		assertFalse(scope.canReadClassroom(99L));
		assertEquals(Set.of(50L), scope.ownStudentIds());
	}

	private void stubRecords(TeachingAssignments teaching, Set<Long> ownStudentIds) {
		when(teachingAssignmentResolver.forUser(USER_ID, TENANT_ID)).thenReturn(teaching);
		when(ownStudentResolver.forUser(USER_ID, TENANT_ID)).thenReturn(ownStudentIds);
	}

	private void authenticate() {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(USER_ID);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}
}
