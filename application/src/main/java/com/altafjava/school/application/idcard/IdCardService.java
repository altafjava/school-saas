package com.altafjava.school.application.idcard;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.document.DefaultDocumentTemplate;
import com.altafjava.platform.application.document.DocumentRenderingService;
import com.altafjava.platform.application.service.FileStorageService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.platform.domain.document.model.PlaceholderField;
import com.altafjava.platform.domain.document.model.PlaceholderFieldType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;

/**
 * Issues student/teacher ID cards through the platform's generic Document Template Engine —
 * consumes {@link DocumentRenderingService} instead of hand-rolling PDF generation, the same
 * relationship {@code CertificateService}/{@code ReportCardService} could eventually have with it.
 * Scoped to students and teachers only for Phase 1 — a staff-wide {@code Employee} entity (Phase 2)
 * is the prerequisite for a general staff ID card, per the roadmap's own sequencing.
 */
@Service
@RequiredArgsConstructor
public class IdCardService {

	public static final String STUDENT_ID_CARD_TYPE = "STUDENT_ID_CARD";
	public static final String TEACHER_ID_CARD_TYPE = "TEACHER_ID_CARD";

	private static final String STUDENT_OWNER_TYPE = "STUDENT";
	private static final String TEACHER_OWNER_TYPE = "TEACHER";

	private final StudentRepository studentRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final ClassroomRepository classroomRepository;
	private final AcademicYearRepository academicYearRepository;
	private final TeacherRepository teacherRepository;
	private final DepartmentRepository departmentRepository;
	private final TenantRepository tenantRepository;
	private final FileStorageService fileStorageService;
	private final DocumentRenderingService documentRenderingService;

	private final DefaultDocumentTemplate defaultStudentIdCardTemplate = loadDefaultTemplate(
			"document-templates/student-id-card.svg",
			List.of(
					new PlaceholderField("tenantName", "School Name", PlaceholderFieldType.TEXT),
					new PlaceholderField("studentName", "Student Name", PlaceholderFieldType.TEXT),
					new PlaceholderField("rollNumberOrAdmissionNumber", "Roll No. / Admission No.",
							PlaceholderFieldType.TEXT),
					new PlaceholderField("className", "Class", PlaceholderFieldType.TEXT),
					new PlaceholderField("photo", "Photo", PlaceholderFieldType.IMAGE),
					new PlaceholderField("qrCode", "QR Code", PlaceholderFieldType.QR)));

	private final DefaultDocumentTemplate defaultTeacherIdCardTemplate = loadDefaultTemplate(
			"document-templates/teacher-id-card.svg",
			List.of(
					new PlaceholderField("tenantName", "School Name", PlaceholderFieldType.TEXT),
					new PlaceholderField("teacherName", "Teacher Name", PlaceholderFieldType.TEXT),
					new PlaceholderField("employeeCode", "Employee Code", PlaceholderFieldType.TEXT),
					new PlaceholderField("department", "Department", PlaceholderFieldType.TEXT),
					new PlaceholderField("photo", "Photo", PlaceholderFieldType.IMAGE),
					new PlaceholderField("qrCode", "QR Code", PlaceholderFieldType.QR)));

	@Transactional
	public DocumentIssuance issueForStudent(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(java.util.UUID.fromString(studentPublicId),
				tenantId).orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));

		Map<String, String> textValues = new HashMap<>();
		textValues.put("tenantName", tenantName(tenantId));
		textValues.put("studentName", student.getFirstName() + " " + student.getLastName());
		textValues.put("qrCode", student.getPublicId().toString());

		StudentClassroomLink currentLink = resolveCurrentLink(tenantId, student.getId()).orElse(null);
		if (currentLink != null && currentLink.getRollNumber() != null) {
			textValues.put("rollNumberOrAdmissionNumber", currentLink.getRollNumber());
		} else {
			textValues.put("rollNumberOrAdmissionNumber", student.getStudentCode());
		}
		textValues.put("className", currentLink != null ? classroomLabel(tenantId, currentLink.getClassroomId()) : "");

		Map<String, byte[]> imageValues = new HashMap<>();
		if (student.getPhotoFilePublicId() != null) {
			imageValues.put("photo",
					fileStorageService.downloadFileForTenant(student.getPhotoFilePublicId().toString()));
		}

		return documentRenderingService.render(tenantId, STUDENT_ID_CARD_TYPE, STUDENT_OWNER_TYPE, student.getId(),
				textValues, imageValues, currentUserId(), defaultStudentIdCardTemplate);
	}

	@Transactional
	public DocumentIssuance issueForTeacher(String teacherPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher teacher = teacherRepository.findByPublicIdAndTenantId(java.util.UUID.fromString(teacherPublicId),
				tenantId).orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherPublicId));

		Map<String, String> textValues = new HashMap<>();
		textValues.put("tenantName", tenantName(tenantId));
		textValues.put("teacherName", teacher.getFirstName() + " " + teacher.getLastName());
		textValues.put("employeeCode", teacher.getEmployeeCode());
		textValues.put("qrCode", teacher.getPublicId().toString());
		textValues.put("department", teacher.getDepartmentId() != null
				? departmentRepository.findByIdAndTenantId(teacher.getDepartmentId(), tenantId)
						.map(Department::getName).orElse("")
				: "");

		Map<String, byte[]> imageValues = new HashMap<>();
		if (teacher.getPhotoFilePublicId() != null) {
			imageValues.put("photo",
					fileStorageService.downloadFileForTenant(teacher.getPhotoFilePublicId().toString()));
		}

		return documentRenderingService.render(tenantId, TEACHER_ID_CARD_TYPE, TEACHER_OWNER_TYPE, teacher.getId(),
				textValues, imageValues, currentUserId(), defaultTeacherIdCardTemplate);
	}

	@Transactional(readOnly = true)
	public DocumentIssuance findByPublicId(String issuancePublicId) {
		return documentRenderingService.findByPublicId(TenantContext.getCurrentTenantId(), issuancePublicId);
	}

	@Transactional(readOnly = true)
	public byte[] downloadPdf(DocumentIssuance issuance) {
		return documentRenderingService.downloadPdf(issuance);
	}

	private String tenantName(Long tenantId) {
		return tenantRepository.findById(tenantId).map(Tenant::getName).orElse("");
	}

	private String classroomLabel(Long tenantId, Long classroomId) {
		return classroomRepository.findByIdAndTenantId(classroomId, tenantId)
				.map(classroom -> classroom.getGrade() + " " + classroom.getSection())
				.orElse("");
	}

	// Mirrors CertificateService#resolveCurrentLink: prefers the current academic year's link,
	// falling back to the most recently enrolled one if no link exists for the current year.
	private Optional<StudentClassroomLink> resolveCurrentLink(Long tenantId, Long studentId) {
		List<StudentClassroomLink> links = studentClassroomLinkRepository.findByStudentId(tenantId, studentId);
		if (links.isEmpty()) {
			return Optional.empty();
		}
		Optional<Long> currentAcademicYearId = academicYearRepository.findByCurrentTrueAndTenantId(tenantId)
				.map(AcademicYear::getId);
		Optional<StudentClassroomLink> currentYearLink = currentAcademicYearId
				.flatMap(yearId -> links.stream().filter(link -> link.getAcademicYearId().equals(yearId)).findFirst());
		return currentYearLink
				.or(() -> links.stream().max(Comparator.comparing(StudentClassroomLink::getEnrolledAt)));
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		return null;
	}

	private DefaultDocumentTemplate loadDefaultTemplate(String classpathLocation, List<PlaceholderField> schema) {
		try {
			String svgContent = new ClassPathResource(classpathLocation).getContentAsString(StandardCharsets.UTF_8);
			return new DefaultDocumentTemplate(svgContent, schema);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load default document template: " + classpathLocation, e);
		}
	}
}
