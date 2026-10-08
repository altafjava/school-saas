package com.altafjava.school.application.idcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.altafjava.platform.application.service.FileStorageService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class IdCardServiceTest {

	private static final Long ISSUER = 99L;

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private DepartmentRepository departmentRepository;
	@Mock
	private FileStorageService fileStorageService;
	@Mock
	private DocumentIssuanceService documentIssuanceService;

	private IdCardService idCardService;

	@BeforeEach
	void setUp() {
		StudentPlacementResolver placementResolver = new StudentPlacementResolver(studentClassroomLinkRepository,
				classroomRepository, academicYearRepository);
		idCardService = new IdCardService(studentRepository, employeeRepository, departmentRepository,
				placementResolver, fileStorageService, documentIssuanceService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student student(long id, String code, UUID publicId) {
		Student student = Student.create(code, "Jane", "Doe", "jane@school.test", LocalDate.of(2012, 1, 1));
		student.setId(id);
		student.setPublicId(publicId);
		return student;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> issuedModel(String documentType, String ownerType, long ownerId) {
		ArgumentCaptor<DocumentIssueRequest> captor = ArgumentCaptor.forClass(DocumentIssueRequest.class);
		verify(documentIssuanceService).issue(captor.capture());
		DocumentIssueRequest request = captor.getValue();
		assertEquals(documentType, request.documentType());
		assertEquals(ownerType, request.ownerEntityType());
		assertEquals(ownerId, request.ownerEntityId());
		assertEquals(ISSUER, request.issuedByUserId());
		return (Map<String, Object>) request.model();
	}

	@Test
	void issueForStudent_withNoClassroomLink_usesStudentCodeAsRollNumber() {
		UUID publicId = UUID.randomUUID();
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L))
				.thenReturn(Optional.of(student(10L, "STU-100", publicId)));
		when(studentClassroomLinkRepository.findByStudentId(1L, 10L)).thenReturn(List.of());
		DocumentIssuance issuance = mock(DocumentIssuance.class);
		when(documentIssuanceService.issue(any(DocumentIssueRequest.class))).thenReturn(issuance);

		assertSame(issuance, idCardService.issueForStudent(publicId.toString(), ISSUER));

		Map<String, Object> model = issuedModel(SchoolDocumentTypes.STUDENT_ID_CARD, "STUDENT", 10L);
		assertEquals("Jane Doe", model.get("studentName"));
		assertEquals("STU-100", model.get("rollNumberOrAdmissionNumber"));
		assertEquals("", model.get("className"));
	}

	@Test
	void issueForStudent_withRollNumber_prefersItAndPrintsTheClass() {
		UUID publicId = UUID.randomUUID();
		StudentClassroomLink link = StudentClassroomLink.create(11L, 20L, 30L, LocalDate.now());
		link.assignRollNumber("07");
		Classroom classroom = Classroom.builder().grade("5").section("A").build();
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L))
				.thenReturn(Optional.of(student(11L, "STU-101", publicId)));
		when(studentClassroomLinkRepository.findByStudentId(1L, 11L)).thenReturn(List.of(link));
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.empty());
		when(classroomRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(classroom));
		when(academicYearRepository.findByIdAndTenantId(30L, 1L)).thenReturn(Optional.empty());

		idCardService.issueForStudent(publicId.toString(), ISSUER);

		Map<String, Object> model = issuedModel(SchoolDocumentTypes.STUDENT_ID_CARD, "STUDENT", 11L);
		assertEquals("07", model.get("rollNumberOrAdmissionNumber"));
		assertEquals("5 A", model.get("className"));
	}

	@Test
	void issueForStudent_withPhoto_loadsItThroughTheTenantScopedFileService() {
		UUID publicId = UUID.randomUUID();
		UUID photoId = UUID.randomUUID();
		Student student = student(12L, "STU-102", publicId);
		student.updatePhoto(photoId);
		byte[] photo = { 1, 2, 3 };
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentId(1L, 12L)).thenReturn(List.of());
		when(fileStorageService.downloadFileForTenant(photoId.toString())).thenReturn(photo);

		idCardService.issueForStudent(publicId.toString(), ISSUER);

		assertSame(photo, issuedModel(SchoolDocumentTypes.STUDENT_ID_CARD, "STUDENT", 12L).get("photo"));
	}

	@Test
	void issueForEmployee_buildsTheStaffCardModel_forAnyKindOfStaff() {
		UUID publicId = UUID.randomUUID();
		Employee driver = Employee.create(StaffCategory.SUPPORT, "EMP-1", "Sam", "Lee", "sam@school.test",
				LocalDate.of(2020, 1, 1));
		driver.setId(50L);
		driver.assignHrDetails(null, "Bus Driver", null, null);
		when(employeeRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(driver));

		idCardService.issueForEmployee(publicId.toString(), ISSUER);

		Map<String, Object> model = issuedModel(SchoolDocumentTypes.STAFF_ID_CARD, "EMPLOYEE", 50L);
		assertEquals("Sam Lee", model.get("employeeName"));
		assertEquals("EMP-1", model.get("employeeCode"));
		assertEquals("Bus Driver", model.get("designation"));
	}

	@Test
	void issueForEmployee_toSomeoneWhoHasLeft_isRefused() {
		UUID publicId = UUID.randomUUID();
		Employee leaver = Employee.create(StaffCategory.SUPPORT, "EMP-2", "Old", "Hand", "old@school.test",
				LocalDate.of(2018, 1, 1));
		leaver.exit(EmployeeStatus.RETIRED, LocalDate.now(), null);
		when(employeeRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(leaver));

		assertThrows(BusinessException.class, () -> idCardService.issueForEmployee(publicId.toString(), ISSUER));
	}

	@Test
	void findStudentCard_isScopedToTheStudentInTheUrl() {
		UUID publicId = UUID.randomUUID();
		when(studentRepository.findByPublicIdAndTenantId(publicId, 1L))
				.thenReturn(Optional.of(student(10L, "STU-100", publicId)));
		when(documentIssuanceService.findForOwner(1L, "issuance-1", "STUDENT", 10L))
				.thenThrow(new ResourceNotFoundException("Document not found"));

		assertThrows(ResourceNotFoundException.class,
				() -> idCardService.findStudentCard(publicId.toString(), "issuance-1"));
	}
}
