package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.reference.UserReferenceResolver;
import com.altafjava.school.domain.common.model.Address;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

	private static final UUID GUARDIAN_PUBLIC_ID = UUID.randomUUID();
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private StudentGuardianLinkRepository studentGuardianLinkRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private EventPublisher eventPublisher;

	@Mock
	private UserReferenceResolver userReferenceResolver;

	private GuardianService guardianService;

	@BeforeEach
	void setUp() {
		guardianService = new GuardianService(guardianRepository, studentGuardianLinkRepository, studentRepository,
				eventPublisher, userReferenceResolver);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_linksTheGuardianToTheUserNamedByPublicId() {
		String userPublicId = UUID.randomUUID().toString();
		when(userReferenceResolver.requireUserId(userPublicId)).thenReturn(42L);
		when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));

		Guardian guardian = guardianService.create("Jane", "Doe", "jane@school.test", "+14155552671", userPublicId);

		assertEquals(42L, guardian.getUserId());
	}

	@Test
	void create_withValidPhone_savesGuardian() {
		when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));

		Guardian guardian = guardianService.create("Jane", "Doe", "jane@school.test", "+14155552671", null);

		assertEquals("+14155552671", guardian.getPhone());
	}

	@Test
	void create_withInvalidPhone_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> guardianService.create("Jane", "Doe", "jane@school.test", "not-a-phone", null));

		verify(guardianRepository, never()).save(any());
	}

	@Test
	void updatePhone_withValidNumber_savesPhone() {
		Guardian guardian = guardianOf(10L);
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));
		when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));

		Guardian updated = guardianService.updatePhone(GUARDIAN_PUBLIC_ID.toString(), "+14155552671");

		assertEquals("+14155552671", updated.getPhone());
	}

	@Test
	void updatePhone_withInvalidNumber_throwsBusinessException() {
		Guardian guardian = guardianOf(10L);
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));

		assertThrows(BusinessException.class,
				() -> guardianService.updatePhone(GUARDIAN_PUBLIC_ID.toString(), "not-a-phone"));
	}

	@Test
	void updateAddress_setsStructuredAddress() {
		Guardian guardian = guardianOf(10L);
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));
		when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));
		Address address = Address.builder().line1("42 Wallaby Way").locality("Sydney").postalCode("2000")
				.countryCode("AU").build();

		Guardian updated = guardianService.updateAddress(GUARDIAN_PUBLIC_ID.toString(), address);

		assertEquals("Sydney", updated.getAddress().getLocality());
		assertEquals("AU", updated.getAddress().getCountryCode());
	}

	@Test
	void linkToStudent_withNonExistentGuardian_throwsResourceNotFound() {
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> guardianService.linkToStudent(GUARDIAN_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString(),
						RelationshipType.MOTHER, true));

		verify(studentGuardianLinkRepository, never()).save(any());
	}

	@Test
	void linkToStudent_withNonExistentStudent_throwsResourceNotFound() {
		Guardian guardian = guardianOf(10L);
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> guardianService.linkToStudent(GUARDIAN_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString(),
						RelationshipType.MOTHER, true));

		verify(studentGuardianLinkRepository, never()).save(any());
	}

	@Test
	void linkToStudent_alreadyLinked_throwsIllegalArgument() {
		Guardian guardian = guardianOf(10L);
		Student student = studentOf(20L);
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(10L, 20L, 1L))
				.thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> guardianService.linkToStudent(GUARDIAN_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString(),
						RelationshipType.MOTHER, true));
	}

	@Test
	void linkToStudent_withValidReferences_savesLinkAndPublishesEvent() {
		Guardian guardian = guardianOf(10L);
		Student student = studentOf(20L);
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(10L, 20L, 1L))
				.thenReturn(false);
		when(studentGuardianLinkRepository.save(any(StudentGuardianLink.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> guardianService.linkToStudent(GUARDIAN_PUBLIC_ID.toString(),
				STUDENT_PUBLIC_ID.toString(), RelationshipType.MOTHER, true));

		verify(eventPublisher).publish(any());
	}

	@Test
	void grantConsent_withExistingLink_stampsConsentGivenAt() {
		Guardian guardian = guardianOf(10L);
		Student student = studentOf(20L);
		StudentGuardianLink link = StudentGuardianLink.create(20L, 10L, RelationshipType.MOTHER, true);
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(studentGuardianLinkRepository.findByGuardianIdAndStudentIdAndTenantId(10L, 20L, 1L))
				.thenReturn(Optional.of(link));
		when(studentGuardianLinkRepository.save(link)).thenReturn(link);

		StudentGuardianLink result = guardianService.grantConsent(GUARDIAN_PUBLIC_ID.toString(),
				STUDENT_PUBLIC_ID.toString());

		assertNotNull(result.getConsentGivenAt());
	}

	@Test
	void revokeConsent_withConsentedLink_clearsConsentGivenAt() {
		Guardian guardian = guardianOf(10L);
		Student student = studentOf(20L);
		StudentGuardianLink link = StudentGuardianLink.create(20L, 10L, RelationshipType.MOTHER, true);
		link.giveConsent();
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(studentGuardianLinkRepository.findByGuardianIdAndStudentIdAndTenantId(10L, 20L, 1L))
				.thenReturn(Optional.of(link));
		when(studentGuardianLinkRepository.save(link)).thenReturn(link);

		StudentGuardianLink result = guardianService.revokeConsent(GUARDIAN_PUBLIC_ID.toString(),
				STUDENT_PUBLIC_ID.toString());

		assertNull(result.getConsentGivenAt());
	}

	@Test
	void grantConsent_withNoExistingLink_throwsResourceNotFound() {
		Guardian guardian = guardianOf(10L);
		Student student = studentOf(20L);
		when(guardianRepository.findByPublicIdAndTenantId(GUARDIAN_PUBLIC_ID, 1L)).thenReturn(Optional.of(guardian));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(studentGuardianLinkRepository.findByGuardianIdAndStudentIdAndTenantId(10L, 20L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> guardianService.grantConsent(GUARDIAN_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));
	}

	private Guardian guardianOf(Long id) {
		Guardian guardian = Guardian.create("Jane", "Doe", "jane@school.test", "555-0100", null);
		guardian.setId(id);
		return guardian;
	}

	private Student studentOf(Long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}
}
