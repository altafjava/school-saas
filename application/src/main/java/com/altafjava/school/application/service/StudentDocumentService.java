package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;
import com.altafjava.school.domain.document.model.StudentDocument;
import com.altafjava.school.domain.document.repository.StudentDocumentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * CRUD + verification workflow for {@link StudentDocument} — reused for both admission-time
 * document uploads and later student-record documents (see that entity's Javadoc). Does not touch
 * platform's {@code FileStorageService} directly: the caller uploads and confirms the file via the
 * generic {@code /api/v1/files} flow first and supplies the resulting {@code filePublicId} here,
 * the same division of responsibility as {@code Student.photoFilePublicId}.
 */
@Service
@RequiredArgsConstructor
public class StudentDocumentService {

	private final StudentDocumentRepository studentDocumentRepository;
	private final StudentRepository studentRepository;
	private final AdmissionRepository admissionRepository;

	@Transactional
	public StudentDocument uploadForStudent(String studentPublicId, String documentType, String filePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		StudentDocument document = StudentDocument.forStudent(student.getId(), documentType,
				UUID.fromString(filePublicId));
		return studentDocumentRepository.save(document);
	}

	@Transactional
	public StudentDocument uploadForAdmission(String admissionPublicId, String documentType, String filePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Admission admission = admissionRepository
				.findByPublicIdAndTenantId(UUID.fromString(admissionPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Admission not found: " + admissionPublicId));
		StudentDocument document = StudentDocument.forAdmission(admission.getId(), documentType,
				UUID.fromString(filePublicId));
		return studentDocumentRepository.save(document);
	}

	@Transactional(readOnly = true)
	public Page<StudentDocument> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return studentDocumentRepository.findByStudentIdAndTenantId(tenantId, student.getId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<StudentDocument> listForAdmission(String admissionPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Admission admission = admissionRepository
				.findByPublicIdAndTenantId(UUID.fromString(admissionPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Admission not found: " + admissionPublicId));
		return studentDocumentRepository.findByAdmissionIdAndTenantId(tenantId, admission.getId(), pageable);
	}

	@Transactional
	public StudentDocument verify(String documentPublicId, Long verifiedByUserId) {
		StudentDocument document = findByPublicId(documentPublicId);
		document.verify(verifiedByUserId);
		return studentDocumentRepository.save(document);
	}

	@Transactional
	public StudentDocument reject(String documentPublicId, Long verifiedByUserId, String rejectionReason) {
		StudentDocument document = findByPublicId(documentPublicId);
		document.reject(verifiedByUserId, rejectionReason);
		return studentDocumentRepository.save(document);
	}

	private StudentDocument findByPublicId(String documentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return studentDocumentRepository.findByPublicIdAndTenantId(tenantId, UUID.fromString(documentPublicId))
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentPublicId));
	}
}
