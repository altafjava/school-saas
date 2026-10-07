package com.altafjava.school.application.idcard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.application.service.FileStorageService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
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

	private final StudentRepository studentRepository;
	private final TeacherRepository teacherRepository;
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

		return documentIssuanceService.issue(new DocumentIssueRequest(tenantId, SchoolDocumentTypes.STUDENT_ID_CARD,
				SchoolDocumentTypes.OWNER_STUDENT, student.getId(), "Student ID Card",
				model.get("studentName").toString(),
				model, issuedByUserId));
	}

	public DocumentIssuance issueForTeacher(String teacherPublicId, Long issuedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher teacher = requireTeacher(tenantId, teacherPublicId);

		Map<String, Object> model = new HashMap<>();
		model.put("teacherName", teacher.getFirstName() + " " + teacher.getLastName());
		model.put("employeeCode", teacher.getEmployeeCode());
		model.put("department", teacher.getDepartmentId() == null ? ""
				: departmentRepository.findByIdAndTenantId(teacher.getDepartmentId(), tenantId)
						.map(Department::getName).orElse(""));
		putPhoto(model, teacher.getPhotoFilePublicId());

		return documentIssuanceService.issue(new DocumentIssueRequest(tenantId, SchoolDocumentTypes.TEACHER_ID_CARD,
				SchoolDocumentTypes.OWNER_TEACHER, teacher.getId(), "Staff ID Card",
				model.get("teacherName").toString(),
				model, issuedByUserId));
	}

	public DocumentIssuance findStudentCard(String studentPublicId, String issuancePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		return documentIssuanceService.findForOwner(tenantId, issuancePublicId, SchoolDocumentTypes.OWNER_STUDENT,
				student.getId());
	}

	public DocumentIssuance findTeacherCard(String teacherPublicId, String issuancePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher teacher = requireTeacher(tenantId, teacherPublicId);
		return documentIssuanceService.findForOwner(tenantId, issuancePublicId, SchoolDocumentTypes.OWNER_TEACHER,
				teacher.getId());
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

	private Teacher requireTeacher(Long tenantId, String teacherPublicId) {
		return teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherPublicId));
	}
}
