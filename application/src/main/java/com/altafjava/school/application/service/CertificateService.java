package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.model.Page;
import com.altafjava.platform.core.model.Pageable;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.certificate.model.CertificateType;
import com.altafjava.school.domain.certificate.repository.CertificateTypeRepository;
import com.altafjava.school.domain.certificate.service.CertificatePlaceholderResolver;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Issues student certificates through the platform Document Template Engine. The tenant's
 * {@link CertificateType} supplies the wording (resolved per student here); layout comes from the
 * tenant's {@code CERTIFICATE.<code>} or generic {@code CERTIFICATE} template, else the built-in
 * default. Downloads and revocation are scoped to the student in the URL.
 */
@Service
@RequiredArgsConstructor
public class CertificateService {

	private final CertificateTypeRepository certificateTypeRepository;
	private final StudentRepository studentRepository;
	private final StudentPlacementResolver placementResolver;
	private final DocumentIssuanceService documentIssuanceService;

	@Transactional(readOnly = true)
	public Page<DocumentIssuance> listForStudent(String studentPublicId,
			org.springframework.data.domain.Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(studentPublicId, tenantId);
		return documentIssuanceService.listForOwner(tenantId, SchoolDocumentTypes.OWNER_STUDENT, student.getId(),
				SchoolDocumentTypes.CERTIFICATE, new Pageable(pageable.getPageNumber(), pageable.getPageSize(), null));
	}

	public DocumentIssuance findByPublicId(String studentPublicId, String certificatePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(studentPublicId, tenantId);
		DocumentIssuance issuance = documentIssuanceService.findForOwner(tenantId, certificatePublicId,
				SchoolDocumentTypes.OWNER_STUDENT, student.getId());
		if (!issuance.getDocumentType().startsWith(SchoolDocumentTypes.CERTIFICATE + ".")) {
			throw new ResourceNotFoundException("Certificate not found: " + certificatePublicId);
		}
		return issuance;
	}

	public byte[] downloadPdf(DocumentIssuance issuance) {
		return documentIssuanceService.downloadPdf(issuance);
	}

	public DocumentIssuance issue(String studentPublicId, String certificateTypePublicId, Long issuedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(studentPublicId, tenantId);
		CertificateType type = certificateTypeRepository
				.findByPublicIdAndTenantId(UUID.fromString(certificateTypePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Certificate type not found: " + certificateTypePublicId));
		if (!type.isActive()) {
			throw new BusinessException("Certificate type is not active: " + type.getName());
		}

		Map<String, String> values = buildValues(tenantId, student, type);
		Map<String, Object> model = new HashMap<>(values);
		model.put("body", CertificatePlaceholderResolver.resolve(type.getWording(), values));
		return documentIssuanceService.issue(new DocumentIssueRequest(tenantId, type.documentType(),
				SchoolDocumentTypes.OWNER_STUDENT, student.getId(), type.getName(), values.get("studentName"), model,
				issuedByUserId));
	}

	@Transactional
	public DocumentIssuance revoke(String studentPublicId, String certificatePublicId, String reason) {
		return documentIssuanceService.revoke(findByPublicId(studentPublicId, certificatePublicId), reason);
	}

	// Wording may reference any of these; unplaced students still get a certificate, with the
	// class/year tokens empty rather than blocking issuance.
	private Map<String, String> buildValues(Long tenantId, Student student, CertificateType type) {
		Map<String, String> values = new HashMap<>();
		values.put("studentName", student.getFirstName() + " " + student.getLastName());
		values.put("studentCode", student.getStudentCode());
		values.put("certificateName", type.getName());
		values.put("issueDate", LocalDate.now().toString());
		var placement = placementResolver.resolve(tenantId, student.getId());
		values.put("className", placement.map(StudentPlacementResolver.Placement::classLabel).orElse(""));
		values.put("academicYear",
				placement.flatMap(p -> p.academicYear().map(AcademicYear::getName)).orElse(""));
		values.put("admissionDate", placement
				.flatMap(p -> p.allLinks().stream().map(StudentClassroomLink::getEnrolledAt)
						.min(Comparator.naturalOrder()))
				.map(Object::toString).orElse(""));
		return values;
	}

	private Student requireStudent(String studentPublicId, Long tenantId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
