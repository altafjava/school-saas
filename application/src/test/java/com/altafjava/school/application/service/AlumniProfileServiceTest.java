package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.altafjava.school.application.lifecycle.LifecycleRecorder;
import com.altafjava.school.domain.alumni.model.AlumniProfile;
import com.altafjava.school.domain.alumni.repository.AlumniProfileRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AlumniProfileServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private AlumniProfileRepository alumniProfileRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private LifecycleRecorder lifecycleRecorder;

	private AlumniProfileService alumniProfileService;

	@BeforeEach
	void setUp() {
		alumniProfileService = new AlumniProfileService(alumniProfileRepository, studentRepository, lifecycleRecorder);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student graduatedStudentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2008, 1, 1));
		student.setId(id);
		student.graduate();
		return student;
	}

	@Test
	void create_forGraduatedStudent_succeeds() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(graduatedStudentWithId(10L)));
		when(alumniProfileRepository.existsByStudentIdAndTenantId(10L, 1L)).thenReturn(false);
		when(alumniProfileRepository.save(any(AlumniProfile.class))).thenAnswer(inv -> inv.getArgument(0));

		AlumniProfile profile = assertDoesNotThrow(() -> alumniProfileService.create(STUDENT_PUBLIC_ID.toString(),
				2026, "Software Engineer", "alice@alumni.test", "555-0100"));

		assertEquals(10L, profile.getStudentId());
		assertEquals(2026, profile.getGraduationYear());
	}

	@Test
	void create_forNonGraduatedStudent_throwsBusinessException() {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2008, 1, 1));
		student.setId(10L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));

		assertThrows(BusinessException.class, () -> alumniProfileService.create(STUDENT_PUBLIC_ID.toString(), 2026,
				null, null, null));
	}

	@Test
	void create_profileAlreadyExists_throwsBusinessException() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(graduatedStudentWithId(10L)));
		when(alumniProfileRepository.existsByStudentIdAndTenantId(10L, 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> alumniProfileService.create(STUDENT_PUBLIC_ID.toString(), 2026,
				null, null, null));
	}

	@Test
	void findByPublicId_unknownPublicId_throwsResourceNotFoundException() {
		UUID publicId = UUID.randomUUID();
		when(alumniProfileRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> alumniProfileService.findByPublicId(publicId.toString()));
	}

	@Test
	void updateContactInfo_changesContactDetails() {
		UUID publicId = UUID.randomUUID();
		AlumniProfile profile = AlumniProfile.create(10L, 2026, "Engineer", "alice@alumni.test", "555-0100");
		when(alumniProfileRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(profile));
		when(alumniProfileRepository.save(any(AlumniProfile.class))).thenAnswer(inv -> inv.getArgument(0));

		AlumniProfile updated = alumniProfileService.updateContactInfo(publicId.toString(), "Senior Engineer",
				"alice.smith@alumni.test", "555-0200");

		assertEquals("Senior Engineer", updated.getCurrentOccupation());
	}

	@Test
	void deactivate_setsActiveFalse() {
		UUID publicId = UUID.randomUUID();
		AlumniProfile profile = AlumniProfile.create(10L, 2026, null, null, null);
		when(alumniProfileRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(profile));
		when(alumniProfileRepository.save(any(AlumniProfile.class))).thenAnswer(inv -> inv.getArgument(0));

		AlumniProfile deactivated = alumniProfileService.deactivate(publicId.toString());

		assertEquals(false, deactivated.isActive());
	}
}
