package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.guardian.model.EmergencyContact;
import com.altafjava.school.domain.guardian.repository.EmergencyContactRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID CONTACT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private EmergencyContactRepository emergencyContactRepository;
	@Mock
	private StudentRepository studentRepository;

	private EmergencyContactService service;

	@BeforeEach
	void setUp() {
		service = new EmergencyContactService(emergencyContactRepository, studentRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(20L);
		org.mockito.Mockito.lenient().when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(student));
	}

	@AfterEach
	void tearDown() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void add_savesContactForStudent() {
		when(emergencyContactRepository.save(any(EmergencyContact.class))).thenAnswer(inv -> inv.getArgument(0));

		EmergencyContact contact = service.add(STUDENT_PUBLIC_ID.toString(), "Bob", "Neighbour", "+14155552671", null,
				1);

		assertEquals(20L, contact.getStudentId());
		assertEquals(1, contact.getPriority());
	}

	@Test
	void add_withInvalidPriority_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> service.add(STUDENT_PUBLIC_ID.toString(), "Bob", "Neighbour", "+14155552671", null, 0));

		verify(emergencyContactRepository, never()).save(any());
	}

	@Test
	void remove_contactBelongingToAnotherStudent_throwsResourceNotFound() {
		EmergencyContact other = EmergencyContact.create(99L, "Eve", "Aunt", "+14155552671", null, 1);
		when(emergencyContactRepository.findByPublicIdAndTenantId(CONTACT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(other));

		assertThrows(ResourceNotFoundException.class,
				() -> service.remove(STUDENT_PUBLIC_ID.toString(), CONTACT_PUBLIC_ID.toString(), 7L));

		verify(emergencyContactRepository, never()).save(any());
	}

	@Test
	void remove_ownContact_softDeletesWithActor() {
		EmergencyContact contact = EmergencyContact.create(20L, "Bob", "Neighbour", "+14155552671", null, 1);
		when(emergencyContactRepository.findByPublicIdAndTenantId(CONTACT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(contact));

		service.remove(STUDENT_PUBLIC_ID.toString(), CONTACT_PUBLIC_ID.toString(), 7L);

		assertTrue(contact.isDeleted());
		assertEquals("7", contact.getDeletedBy());
		verify(emergencyContactRepository).save(contact);
	}
}
