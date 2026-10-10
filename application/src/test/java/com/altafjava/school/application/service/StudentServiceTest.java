package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.school.application.lifecycle.LifecycleChange;
import com.altafjava.school.application.lifecycle.LifecycleRecorder;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.domain.common.model.Address;
import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private NumberSequenceService numberSequenceService;
	@Mock
	private LifecycleRecorder lifecycleRecorder;
	@Mock
	private PublicIdLookup publicIdLookup;

	private StudentService studentService;

	@BeforeEach
	void setUp() {
		studentService = new StudentService(studentRepository, numberSequenceService, lifecycleRecorder,
				publicIdLookup);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void enroll_withExplicitStudentCode_usesItAsIsAndSkipsSequence() {
		when(studentRepository.existsByStudentCodeAndTenantId("STU-100", 1L)).thenReturn(false);
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		Student student = studentService.enroll("STU-100", "Henry", "Ford", "henry@school.test",
				LocalDate.of(2011, 1, 1));

		assertEquals("STU-100", student.getStudentCode());
		verifyNoInteractions(numberSequenceService);
	}

	@Test
	void enroll_withoutStudentCode_generatesOneFromTenantSequence() {
		when(numberSequenceService.generateNext(1L, "STUDENT_CODE", "STU-", 4, ResetPeriod.NEVER))
				.thenReturn("STU-0007");
		when(studentRepository.existsByStudentCodeAndTenantId("STU-0007", 1L)).thenReturn(false);
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		Student student = studentService.enroll(null, "Ivy", "Stone", "ivy@school.test", LocalDate.of(2011, 2, 2));

		assertEquals("STU-0007", student.getStudentCode());
	}

	@Test
	void withdraw_transitionsEnrollmentStatusToWithdrawn_withoutSoftDeleting() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-001", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		studentService.withdraw(publicId.toString(), LifecycleChange.NONE);

		ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
		verify(studentRepository).save(captor.capture());
		assertEquals(EnrollmentStatus.WITHDRAWN, captor.getValue().getEnrollmentStatus());
		assertFalse(captor.getValue().isDeleted(),
				"withdraw() must transition enrollmentStatus without soft-deleting — a withdrawn student "
						+ "stays visible to historical/guardian views");
	}

	@Test
	void transfer_transitionsEnrollmentStatusToTransferred_withoutSoftDeleting() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-003", "Carol", "Lee", "carol@school.test", LocalDate.of(2011, 1, 1));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		studentService.transfer(publicId.toString(), LifecycleChange.NONE);

		ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
		verify(studentRepository).save(captor.capture());
		assertEquals(EnrollmentStatus.TRANSFERRED, captor.getValue().getEnrollmentStatus());
		assertFalse(captor.getValue().isDeleted(),
				"transfer() must transition enrollmentStatus without soft-deleting — a transferred student "
						+ "stays visible to historical/guardian views");
	}

	@Test
	void graduate_transitionsEnrollmentStatusToGraduated_withoutSoftDeleting() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-002", "Bob", "Jones", "bob@school.test", LocalDate.of(2009, 1, 1));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		studentService.graduate(publicId.toString(), LifecycleChange.NONE);

		ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
		verify(studentRepository).save(captor.capture());
		assertEquals(EnrollmentStatus.GRADUATED, captor.getValue().getEnrollmentStatus());
		assertFalse(captor.getValue().isDeleted());
	}

	@Test
	void updateContactDetails_replacesMutableFields() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-003", "Carol", "Lee", "carol@school.test", LocalDate.of(2010, 5, 5));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		Student updated = studentService.updateContactDetails(publicId.toString(), "Carolyn", "Jones",
				"carolyn@school.test", LocalDate.of(2010, 6, 6), Gender.FEMALE, ExpectedVersion.any());

		assertEquals("Carolyn", updated.getFirstName());
		assertEquals("Jones", updated.getLastName());
		assertEquals("carolyn@school.test", updated.getEmail());
		assertEquals(Gender.FEMALE, updated.getGender());
	}

	@Test
	void updatePhone_withValidNumberAndNoAddress_savesPhoneInInternationalFormat() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-004", "Dana", "White", "dana@school.test", LocalDate.of(2010, 3, 3));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		Student updated = studentService.updatePhone(publicId.toString(), "+14155552671", ExpectedVersion.any());

		assertEquals("+14155552671", updated.getPhone());
	}

	@Test
	void updatePhone_withInvalidNumber_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-005", "Eve", "Adams", "eve@school.test", LocalDate.of(2010, 4, 4));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));

		assertThrows(BusinessException.class,
				() -> studentService.updatePhone(publicId.toString(), "not-a-phone", ExpectedVersion.any()));
	}

	@Test
	void updatePhone_withRegionalNumberMatchingExistingAddressCountry_isValid() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-006", "Frank", "Green", "frank@school.test", LocalDate.of(2010, 5, 5));
		student.updateAddress(Address.builder().line1("1 Infinite Loop").countryCode("US").build());
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		Student updated = studentService.updatePhone(publicId.toString(), "(415) 555-2671", ExpectedVersion.any());

		assertEquals("(415) 555-2671", updated.getPhone());
	}

	@Test
	void updateAddress_setsStructuredAddress() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-007", "Grace", "Hall", "grace@school.test", LocalDate.of(2010, 6, 6));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
		Address address = Address.builder().line1("221B Baker Street").locality("London").postalCode("NW1 6XE")
				.countryCode("GB").build();

		Student updated = studentService.updateAddress(publicId.toString(), address, ExpectedVersion.any());

		assertEquals("London", updated.getAddress().getLocality());
		assertEquals("GB", updated.getAddress().getCountryCode());
	}

	@Test
	void updatePhoto_withFilePublicId_setsPhotoReference() {
		UUID publicId = UUID.randomUUID();
		UUID filePublicId = UUID.randomUUID();
		Student student = Student.create("STU-008", "Hank", "Iron", "hank@school.test", LocalDate.of(2010, 7, 7));
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		Student updated = studentService.updatePhoto(publicId.toString(), filePublicId.toString(),
				ExpectedVersion.any());

		assertEquals(filePublicId, updated.getPhotoFilePublicId());
	}

	@Test
	void updatePhoto_withNull_clearsPhotoReference() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-009", "Ida", "Jones", "ida@school.test", LocalDate.of(2010, 8, 8));
		student.updatePhoto(UUID.randomUUID());
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		Student updated = studentService.updatePhoto(publicId.toString(), null, ExpectedVersion.any());

		assertEquals(null, updated.getPhotoFilePublicId());
	}

	@Test
	void listStudents_withStatusFilter_delegatesToStatusFilteredQuery() {
		when(studentRepository.findAllByTenantIdAndEnrollmentStatus(1L, EnrollmentStatus.ACTIVE,
				org.springframework.data.domain.PageRequest.of(0, 20)))
				.thenReturn(org.springframework.data.domain.Page.empty());

		studentService.listStudents(org.springframework.data.domain.PageRequest.of(0, 20), EnrollmentStatus.ACTIVE);

		verify(studentRepository).findAllByTenantIdAndEnrollmentStatus(1L, EnrollmentStatus.ACTIVE,
				org.springframework.data.domain.PageRequest.of(0, 20));
	}
}
