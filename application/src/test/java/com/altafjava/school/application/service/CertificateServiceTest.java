package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.certificate.model.CertificateType;
import com.altafjava.school.domain.certificate.repository.CertificateTypeRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID TYPE_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private CertificateTypeRepository certificateTypeRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private DocumentIssuanceService documentIssuanceService;

	private CertificateService certificateService;

	@BeforeEach
	void setUp() {
		certificateService = new CertificateService(certificateTypeRepository, studentRepository,
				new StudentPlacementResolver(studentClassroomLinkRepository, classroomRepository,
						academicYearRepository),
				documentIssuanceService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student student() {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(1L);
		student.setPublicId(STUDENT_PUBLIC_ID);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		return student;
	}

	private CertificateType type(boolean active) {
		CertificateType type = CertificateType.create("BONAFIDE", "Bonafide Certificate",
				"This certifies {{studentName}} of {{className}}, admitted {{admissionDate}}.");
		type.setPublicId(TYPE_PUBLIC_ID);
		if (!active) {
			type.deactivate();
		}
		when(certificateTypeRepository.findByPublicIdAndTenantId(TYPE_PUBLIC_ID, 1L)).thenReturn(Optional.of(type));
		return type;
	}

	@Test
	void issue_resolvesWordingAndIssuesUnderTheTypesDocumentType() {
		student();
		type(true);
		StudentClassroomLink link = StudentClassroomLink.create(1L, 5L, 7L, LocalDate.of(2020, 6, 1));
		AcademicYear year = AcademicYear.create("2025-26", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 4, 30), true);
		year.setId(7L);
		when(studentClassroomLinkRepository.findByStudentId(1L, 1L)).thenReturn(List.of(link));
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.of(year));
		when(classroomRepository.findByIdAndTenantId(5L, 1L))
				.thenReturn(Optional.of(Classroom.create("5A", "Grade 5", "A", 7L, "2025-26", null)));
		when(academicYearRepository.findByIdAndTenantId(7L, 1L)).thenReturn(Optional.of(year));
		DocumentIssuance issuance = org.mockito.Mockito.mock(DocumentIssuance.class);
		when(documentIssuanceService.issue(any(DocumentIssueRequest.class))).thenReturn(issuance);

		assertEquals(issuance, certificateService.issue(STUDENT_PUBLIC_ID.toString(), TYPE_PUBLIC_ID.toString(), 99L));

		ArgumentCaptor<DocumentIssueRequest> captor = ArgumentCaptor.forClass(DocumentIssueRequest.class);
		verify(documentIssuanceService).issue(captor.capture());
		DocumentIssueRequest request = captor.getValue();
		assertEquals("CERTIFICATE.BONAFIDE", request.documentType());
		assertEquals("STUDENT", request.ownerEntityType());
		assertEquals("Bonafide Certificate", request.title());
		assertEquals("Alice Smith", request.subjectDisplayName());
		Map<String, ?> model = request.model();
		assertEquals("Alice Smith", model.get("studentName"));
		assertEquals("2025-26", model.get("academicYear"));
		String body = (String) model.get("body");
		assertTrue(body.startsWith("This certifies Alice Smith of Grade 5 A, admitted "), body);
	}

	@Test
	void issue_studentWithoutPlacement_stillIssuesWithEmptyClassTokens() {
		student();
		type(true);
		when(studentClassroomLinkRepository.findByStudentId(1L, 1L)).thenReturn(List.of());
		when(documentIssuanceService.issue(any(DocumentIssueRequest.class))).thenReturn(null);

		certificateService.issue(STUDENT_PUBLIC_ID.toString(), TYPE_PUBLIC_ID.toString(), 99L);

		ArgumentCaptor<DocumentIssueRequest> captor = ArgumentCaptor.forClass(DocumentIssueRequest.class);
		verify(documentIssuanceService).issue(captor.capture());
		assertEquals("This certifies Alice Smith of , admitted .", captor.getValue().model().get("body"));
	}

	@Test
	void issue_inactiveType_throwsBusinessException() {
		student();
		type(false);

		assertThrows(BusinessException.class,
				() -> certificateService.issue(STUDENT_PUBLIC_ID.toString(), TYPE_PUBLIC_ID.toString(), 99L));

		verify(documentIssuanceService, never()).issue(any());
	}

	@Test
	void issue_unknownType_throwsResourceNotFound() {
		student();
		when(certificateTypeRepository.findByPublicIdAndTenantId(TYPE_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> certificateService.issue(STUDENT_PUBLIC_ID.toString(), TYPE_PUBLIC_ID.toString(), 99L));
	}

	@Test
	void findByPublicId_rejectsNonCertificateDocumentsOfTheSameStudent() {
		student();
		DocumentIssuance idCard = DocumentIssuance.create("STUDENT_ID_CARD", "STUDENT", 1L, "ID", null, null, null,
				"code", "key", 1L);
		when(documentIssuanceService.findForOwner(1L, "doc-1", "STUDENT", 1L)).thenReturn(idCard);

		assertThrows(ResourceNotFoundException.class,
				() -> certificateService.findByPublicId(STUDENT_PUBLIC_ID.toString(), "doc-1"));
	}

	@Test
	void revoke_revokesTheStudentsOwnCertificate() {
		student();
		DocumentIssuance certificate = DocumentIssuance.create("CERTIFICATE.BONAFIDE", "STUDENT", 1L, "Bonafide", null,
				null, null, "code", "key", 1L);
		when(documentIssuanceService.findForOwner(1L, "doc-1", "STUDENT", 1L)).thenReturn(certificate);

		certificateService.revoke(STUDENT_PUBLIC_ID.toString(), "doc-1", "Issued in error");

		verify(documentIssuanceService).revoke(certificate, "Issued in error");
	}
}
