package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class OwnStudentResolverTest {

	private static final Long TENANT_ID = 1L;
	private static final Long USER_ID = 7L;

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private StudentGuardianLinkRepository studentGuardianLinkRepository;
	@InjectMocks
	private OwnStudentResolver resolver;

	@Test
	void forUser_withNoSchoolRecords_ownsNoStudent() {
		when(studentRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());
		when(guardianRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());

		assertTrue(resolver.forUser(USER_ID, TENANT_ID).isEmpty());
	}

	@Test
	void forUser_asStudent_ownsTheirOwnRecord() {
		Student student = mock(Student.class);
		when(student.getId()).thenReturn(50L);
		when(studentRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());

		assertEquals(Set.of(50L), resolver.forUser(USER_ID, TENANT_ID));
	}

	@Test
	void forUser_asGuardian_ownsLinkedChildrenExceptCustodyRestricted() {
		Guardian guardian = mock(Guardian.class);
		when(guardian.getId()).thenReturn(9L);
		when(studentRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());
		when(guardianRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(guardian));
		StudentGuardianLink allowed = mock(StudentGuardianLink.class);
		when(allowed.getStudentId()).thenReturn(51L);
		StudentGuardianLink restricted = mock(StudentGuardianLink.class);
		when(restricted.isCustodyRestricted()).thenReturn(true);
		when(studentGuardianLinkRepository.findAllByGuardianIdAndTenantId(9L, TENANT_ID))
				.thenReturn(List.of(allowed, restricted));

		assertEquals(Set.of(51L), resolver.forUser(USER_ID, TENANT_ID));
	}

	@Test
	void forUser_withoutUserId_ownsNoStudent() {
		assertTrue(resolver.forUser(null, TENANT_ID).isEmpty());
	}
}
