package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import com.altafjava.school.domain.admission.repository.AdmissionRepository;
import com.altafjava.school.domain.document.model.DocumentVerificationStatus;
import com.altafjava.school.domain.document.model.StudentDocument;
import com.altafjava.school.domain.document.repository.StudentDocumentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentDocumentServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private StudentDocumentRepository studentDocumentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private AdmissionRepository admissionRepository;

	private StudentDocumentService studentDocumentService;

	@BeforeEach
	void setUp() {
		studentDocumentService = new StudentDocumentService(studentDocumentRepository, studentRepository,
				admissionRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void uploadForStudent_createsPendingDocument() {
		UUID filePublicId = UUID.randomUUID();
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(studentDocumentRepository.save(any(StudentDocument.class))).thenAnswer(inv -> inv.getArgument(0));

		StudentDocument document = studentDocumentService.uploadForStudent(STUDENT_PUBLIC_ID.toString(),
				"BIRTH_CERTIFICATE", filePublicId.toString());

		assertEquals(10L, document.getStudentId());
		assertEquals("BIRTH_CERTIFICATE", document.getDocumentType());
		assertEquals(DocumentVerificationStatus.PENDING, document.getVerificationStatus());
	}

	@Test
	void uploadForStudent_withUnknownStudent_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> studentDocumentService
				.uploadForStudent(STUDENT_PUBLIC_ID.toString(), "BIRTH_CERTIFICATE", UUID.randomUUID().toString()));
	}

	@Test
	void verify_pendingDocument_transitionsToVerified() {
		UUID documentPublicId = UUID.randomUUID();
		StudentDocument document = StudentDocument.forStudent(10L, "BIRTH_CERTIFICATE", UUID.randomUUID());
		document.setPublicId(documentPublicId);
		when(studentDocumentRepository.findByPublicIdAndTenantId(1L, documentPublicId))
				.thenReturn(Optional.of(document));
		when(studentDocumentRepository.save(any(StudentDocument.class))).thenAnswer(inv -> inv.getArgument(0));

		StudentDocument result = studentDocumentService.verify(documentPublicId.toString(), 99L);

		assertEquals(DocumentVerificationStatus.VERIFIED, result.getVerificationStatus());
		assertEquals(99L, result.getVerifiedByUserId());
	}

	@Test
	void reject_pendingDocument_requiresReason() {
		UUID documentPublicId = UUID.randomUUID();
		StudentDocument document = StudentDocument.forStudent(10L, "BIRTH_CERTIFICATE", UUID.randomUUID());
		document.setPublicId(documentPublicId);
		when(studentDocumentRepository.findByPublicIdAndTenantId(1L, documentPublicId))
				.thenReturn(Optional.of(document));

		assertThrows(BusinessException.class,
				() -> studentDocumentService.reject(documentPublicId.toString(), 99L, ""));
	}

	@Test
	void verify_alreadyVerifiedDocument_throwsBusinessException() {
		UUID documentPublicId = UUID.randomUUID();
		StudentDocument document = StudentDocument.forStudent(10L, "BIRTH_CERTIFICATE", UUID.randomUUID());
		document.setPublicId(documentPublicId);
		document.verify(1L);
		when(studentDocumentRepository.findByPublicIdAndTenantId(1L, documentPublicId))
				.thenReturn(Optional.of(document));

		assertThrows(BusinessException.class, () -> studentDocumentService.verify(documentPublicId.toString(), 99L));
	}
}
