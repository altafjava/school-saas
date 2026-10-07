package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.GuardianAuthorizationChange;
import com.altafjava.school.domain.guardian.model.PickupDecision;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianAuthorizationChangeRepository;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class GuardianPickupServiceTest {

	private static final UUID GUARDIAN_PUBLIC_ID = UUID.randomUUID();
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final Long ACTING_USER_ID = 77L;

	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentGuardianLinkRepository linkRepository;
	@Mock
	private GuardianAuthorizationChangeRepository changeRepository;

	private GuardianPickupService service;
	private StudentGuardianLink link;

	@BeforeEach
	void setUp() {
		service = new GuardianPickupService(guardianRepository, studentRepository, linkRepository, changeRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);

		Guardian guardian = Guardian.create("Jane", "Doe", "jane@school.test", "+14155552671", null);
		guardian.setId(10L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(20L);
		link = StudentGuardianLink.create(20L, 10L, RelationshipType.MOTHER, true);
		lenient(guardian, student);
	}

	private void lenient(Guardian guardian, Student student) {
		org.mockito.Mockito.lenient().when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(guardian));
		org.mockito.Mockito.lenient().when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(student));
		org.mockito.Mockito.lenient().when(linkRepository.findByGuardianIdAndStudentIdAndTenantId(10L, 20L, 1L))
				.thenReturn(Optional.of(link));
		org.mockito.Mockito.lenient().when(linkRepository.save(any(StudentGuardianLink.class)))
				.thenAnswer(inv -> inv.getArgument(0));
	}

	@AfterEach
	void tearDown() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void authorizePickup_setsFlagAndRecordsHistory() {
		service.authorizePickup(GUARDIAN_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString(), ACTING_USER_ID);

		assertTrue(link.isAuthorizedForPickup());
		ArgumentCaptor<GuardianAuthorizationChange> captor = ArgumentCaptor.forClass(GuardianAuthorizationChange.class);
		verify(changeRepository).save(captor.capture());
		GuardianAuthorizationChange change = captor.getValue();
		assertFalse(change.isOldAuthorizedForPickup());
		assertTrue(change.isNewAuthorizedForPickup());
		assertEquals(ACTING_USER_ID, change.getChangedByUserId());
		assertEquals(1L, change.getTenantId());
	}

	@Test
	void authorizePickup_alreadyAuthorized_recordsNoHistory() {
		link.authorizePickup();

		service.authorizePickup(GUARDIAN_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString(), ACTING_USER_ID);

		verify(changeRepository, never()).save(any());
	}

	@Test
	void restrictCustody_revokesPickupAndRecordsNote() {
		link.authorizePickup();

		service.restrictCustody(GUARDIAN_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString(), "court order",
				ACTING_USER_ID);

		assertEquals(PickupDecision.CUSTODY_RESTRICTED, link.pickupDecision());
		ArgumentCaptor<GuardianAuthorizationChange> captor = ArgumentCaptor.forClass(GuardianAuthorizationChange.class);
		verify(changeRepository).save(captor.capture());
		assertTrue(captor.getValue().isOldAuthorizedForPickup());
		assertFalse(captor.getValue().isNewAuthorizedForPickup());
		assertTrue(captor.getValue().isNewCustodyRestricted());
		assertEquals("court order", captor.getValue().getNote());
	}

	@Test
	void authorizePickup_whileRestricted_throwsAndRecordsNothing() {
		link.restrictCustody("court order");

		assertThrows(BusinessException.class, () -> service.authorizePickup(GUARDIAN_PUBLIC_ID.toString(),
				STUDENT_PUBLIC_ID.toString(), ACTING_USER_ID));

		verify(changeRepository, never()).save(any());
	}

	@Test
	void authorizePickup_withoutLink_throwsResourceNotFound() {
		when(linkRepository.findByGuardianIdAndStudentIdAndTenantId(10L, 20L, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.authorizePickup(GUARDIAN_PUBLIC_ID.toString(),
				STUDENT_PUBLIC_ID.toString(), ACTING_USER_ID));
	}

	@Test
	void check_authorizedGuardian_isAuthorized() {
		link.authorizePickup();

		assertEquals(PickupDecision.AUTHORIZED,
				service.check(STUDENT_PUBLIC_ID.toString(), GUARDIAN_PUBLIC_ID.toString()));
	}

	@Test
	void check_unknownGuardian_failsClosedAsNotLinked() {
		UUID stranger = UUID.randomUUID();
		when(guardianRepository.findByPublicIdAndTenantId(stranger, 1L)).thenReturn(Optional.empty());

		assertEquals(PickupDecision.NOT_LINKED, service.check(STUDENT_PUBLIC_ID.toString(), stranger.toString()));
	}

	@Test
	void check_guardianWithoutLink_isNotLinked() {
		when(linkRepository.findByGuardianIdAndStudentIdAndTenantId(10L, 20L, 1L)).thenReturn(Optional.empty());

		assertEquals(PickupDecision.NOT_LINKED,
				service.check(STUDENT_PUBLIC_ID.toString(), GUARDIAN_PUBLIC_ID.toString()));
	}

	@Test
	void listAuthorized_excludesUnauthorizedAndRestrictedLinks() {
		StudentGuardianLink authorized = StudentGuardianLink.create(20L, 10L, RelationshipType.MOTHER, true);
		authorized.authorizePickup();
		StudentGuardianLink restricted = StudentGuardianLink.create(20L, 11L, RelationshipType.FATHER, false);
		restricted.restrictCustody("court order");
		StudentGuardianLink notAuthorized = StudentGuardianLink.create(20L, 12L, RelationshipType.OTHER, false);
		Guardian guardian = Guardian.create("Jane", "Doe", null, "+14155552671", null);
		guardian.setId(10L);
		when(linkRepository.findByStudentId(1L, 20L)).thenReturn(List.of(authorized, restricted, notAuthorized));
		when(guardianRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(guardian));

		List<GuardianPickupService.PickupAuthorizedGuardian> result = service
				.listAuthorized(STUDENT_PUBLIC_ID.toString());

		assertEquals(1, result.size());
		assertEquals(10L, result.get(0).link().getGuardianId());
	}
}
