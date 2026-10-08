package com.altafjava.school.application.idcard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.application.service.FileStorageService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.model.Pageable;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Issues student and staff ID cards through the platform Document Template Engine. The card's
 * QR code is its verification link, so a gate scan proves the card is genuine and not revoked —
 * the same code can identify the holder for attendance scanning. Downloads are scoped to the
 * card's owner, so one person's URL can never fetch another's document.
 */
@Service
@RequiredArgsConstructor
public class IdCardService {

	private static final int MAX_CARDS_CHECKED = 100;

	private final StudentRepository studentRepository;
	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;
	private final StudentPlacementResolver placementResolver;
	private final FileStorageService fileStorageService;
	private final DocumentIssuanceService documentIssuanceService;

	public DocumentIssuance issueForStudent(String studentPublicId, Long issuedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		var placement = placementResolver.resolve(tenantId, student.getId());
		String rollNumber = placement.map(p -> p.link().getRollNumber()).orElse(null);

		Map<String, Object> model = new HashMap<>();
		model.put("studentName", student.getFirstName() + " " + student.getLastName());
		model.put("rollNumberOrAdmissionNumber", rollNumber != null ? rollNumber : student.getStudentCode());
		model.put("className", placement.map(StudentPlacementResolver.Placement::classLabel).orElse(""));
		putPhoto(model, student.getPhotoFilePublicId());

		DocumentIssuance card = documentIssuanceService.issue(new DocumentIssueRequest(tenantId,
				SchoolDocumentTypes.STUDENT_ID_CARD, SchoolDocumentTypes.OWNER_STUDENT, student.getId(),
				"Student ID Card", model.get("studentName").toString(), model, issuedByUserId));
		revokeSuperseded(tenantId, card, SchoolDocumentTypes.OWNER_STUDENT, student.getId());
		return card;
	}

	public DocumentIssuance issueForEmployee(String employeePublicId, Long issuedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = requireEmployee(tenantId, employeePublicId);
		if (!employee.isActive()) {
			throw new BusinessException("A staff ID card cannot be issued to someone who has left the school");
		}

		Map<String, Object> model = new HashMap<>();
		model.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
		model.put("employeeCode", employee.getEmployeeCode());
		model.put("designation", employee.getDesignation() == null ? "" : employee.getDesignation());
		model.put("department", employee.getDepartmentId() == null ? ""
				: departmentRepository.findByIdAndTenantId(employee.getDepartmentId(), tenantId)
						.map(Department::getName).orElse(""));
		putPhoto(model, employee.getPhotoFilePublicId());

		DocumentIssuance card = documentIssuanceService.issue(new DocumentIssueRequest(tenantId,
				SchoolDocumentTypes.STAFF_ID_CARD, SchoolDocumentTypes.OWNER_EMPLOYEE, employee.getId(),
				"Staff ID Card", model.get("employeeName").toString(), model, issuedByUserId));
		revokeSuperseded(tenantId, card, SchoolDocumentTypes.OWNER_EMPLOYEE, employee.getId());
		return card;
	}

	// A replacement card makes the old one — possibly lost — stop scanning as genuine.
	private void revokeSuperseded(Long tenantId, DocumentIssuance newCard, String ownerType, Long ownerId) {
		documentIssuanceService
				.listForOwner(tenantId, ownerType, ownerId, newCard.getDocumentType(),
						Pageable.of(0, MAX_CARDS_CHECKED))
				.content().stream()
				.filter(card -> !card.getId().equals(newCard.getId()) && !card.isRevoked())
				.forEach(card -> documentIssuanceService.revoke(card, "Replaced by a newly issued card"));
	}

	public DocumentIssuance findStudentCard(String studentPublicId, String issuancePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		return documentIssuanceService.findForOwner(tenantId, issuancePublicId, SchoolDocumentTypes.OWNER_STUDENT,
				student.getId());
	}

	public DocumentIssuance findEmployeeCard(String employeePublicId, String issuancePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = requireEmployee(tenantId, employeePublicId);
		return documentIssuanceService.findForOwner(tenantId, issuancePublicId, SchoolDocumentTypes.OWNER_EMPLOYEE,
				employee.getId());
	}

	public byte[] downloadPdf(DocumentIssuance issuance) {
		return documentIssuanceService.downloadPdf(issuance);
	}

	private void putPhoto(Map<String, Object> model, UUID photoFilePublicId) {
		if (photoFilePublicId != null) {
			model.put("photo", fileStorageService.downloadFileForTenant(photoFilePublicId.toString()));
		}
	}

	private Student requireStudent(Long tenantId, String studentPublicId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}

	private Employee requireEmployee(Long tenantId, String employeePublicId) {
		return employeeRepository.findByPublicIdAndTenantId(UUID.fromString(employeePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeePublicId));
	}
}
