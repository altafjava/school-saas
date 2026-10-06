package com.altafjava.school.application.idcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.document.DefaultDocumentTemplate;
import com.altafjava.platform.application.document.DocumentRenderingService;
import com.altafjava.platform.application.service.FileStorageService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class IdCardServiceTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private DepartmentRepository departmentRepository;
	@Mock
	private TenantRepository tenantRepository;
	@Mock
	private FileStorageService fileStorageService;
	@Mock
	private DocumentRenderingService documentRenderingService;

	private IdCardService idCardService;

	@BeforeEach
	void setUp() {
		idCardService = new IdCardService(studentRepository, studentClassroomLinkRepository, classroomRepository,
				academicYearRepository, teacherRepository, departmentRepository, tenantRepository, fileStorageService,
				documentRenderingService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void issueForStudent_withNoClassroomLink_usesStudentCodeAsRollNumber() {
		UUID studentPublicId = UUID.randomUUID();
		Student student = Student.create("STU-100", "Jane", "Doe", "jane@school.test", LocalDate.of(2012, 1, 1));
		student.setId(10L);
		student.setPublicId(studentPublicId);
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentId(1L, 10L)).thenReturn(List.of());
		when(tenantRepository.findById(1L)).thenReturn(Optional.of(Tenant.builder().name("Test School").build()));
		DocumentIssuance issuance = mock(DocumentIssuance.class);
		when(documentRenderingService.render(eq(1L), eq(IdCardService.STUDENT_ID_CARD_TYPE), eq("STUDENT"), eq(10L),
				anyMap(), anyMap(), any(), any(DefaultDocumentTemplate.class))).thenReturn(issuance);

		DocumentIssuance result = idCardService.issueForStudent(studentPublicId.toString());

		assertEquals(issuance, result);
		ArgumentCaptor<java.util.Map<String, String>> textValuesCaptor = ArgumentCaptor.forClass(java.util.Map.class);
		verify(documentRenderingService).render(eq(1L), eq(IdCardService.STUDENT_ID_CARD_TYPE), eq("STUDENT"), eq(10L),
				textValuesCaptor.capture(), anyMap(), any(), any(DefaultDocumentTemplate.class));
		assertEquals("Jane Doe", textValuesCaptor.getValue().get("studentName"));
		assertEquals("STU-100", textValuesCaptor.getValue().get("rollNumberOrAdmissionNumber"));
	}

	@Test
	void issueForStudent_withClassroomLinkRollNumber_prefersRollNumberOverStudentCode() {
		UUID studentPublicId = UUID.randomUUID();
		Student student = Student.create("STU-101", "John", "Roe", "john@school.test", LocalDate.of(2012, 1, 1));
		student.setId(11L);
		student.setPublicId(studentPublicId);
		StudentClassroomLink link = StudentClassroomLink.create(11L, 20L, 30L, LocalDate.now());
		link.assignRollNumber("07");
		Classroom classroom = Classroom.builder().grade("5").section("A").build();
		classroom.setId(20L);
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentId(1L, 11L)).thenReturn(List.of(link));
		when(academicYearRepository.findByCurrentTrueAndTenantId(1L)).thenReturn(Optional.empty());
		when(classroomRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(classroom));
		when(tenantRepository.findById(1L)).thenReturn(Optional.of(Tenant.builder().name("Test School").build()));
		DocumentIssuance issuance = mock(DocumentIssuance.class);
		when(documentRenderingService.render(eq(1L), eq(IdCardService.STUDENT_ID_CARD_TYPE), eq("STUDENT"), eq(11L),
				anyMap(), anyMap(), any(), any(DefaultDocumentTemplate.class))).thenReturn(issuance);

		idCardService.issueForStudent(studentPublicId.toString());

		ArgumentCaptor<java.util.Map<String, String>> textValuesCaptor = ArgumentCaptor.forClass(java.util.Map.class);
		verify(documentRenderingService).render(eq(1L), eq(IdCardService.STUDENT_ID_CARD_TYPE), eq("STUDENT"), eq(11L),
				textValuesCaptor.capture(), anyMap(), any(), any(DefaultDocumentTemplate.class));
		assertEquals("07", textValuesCaptor.getValue().get("rollNumberOrAdmissionNumber"));
		assertEquals("5 A", textValuesCaptor.getValue().get("className"));
	}

	@Test
	void issueForTeacher_buildsExpectedPlaceholders() {
		UUID teacherPublicId = UUID.randomUUID();
		Teacher teacher = Teacher.create("EMP-1", "Sam", "Lee", "sam@school.test", LocalDate.of(2020, 1, 1));
		teacher.setId(50L);
		teacher.setPublicId(teacherPublicId);
		when(teacherRepository.findByPublicIdAndTenantId(teacherPublicId, 1L)).thenReturn(Optional.of(teacher));
		when(tenantRepository.findById(1L)).thenReturn(Optional.of(Tenant.builder().name("Test School").build()));
		DocumentIssuance issuance = mock(DocumentIssuance.class);
		when(documentRenderingService.render(eq(1L), eq(IdCardService.TEACHER_ID_CARD_TYPE), eq("TEACHER"), eq(50L),
				anyMap(), anyMap(), any(), any(DefaultDocumentTemplate.class))).thenReturn(issuance);

		DocumentIssuance result = idCardService.issueForTeacher(teacherPublicId.toString());

		assertEquals(issuance, result);
		ArgumentCaptor<java.util.Map<String, String>> textValuesCaptor = ArgumentCaptor.forClass(java.util.Map.class);
		verify(documentRenderingService).render(eq(1L), eq(IdCardService.TEACHER_ID_CARD_TYPE), eq("TEACHER"), eq(50L),
				textValuesCaptor.capture(), anyMap(), any(), any(DefaultDocumentTemplate.class));
		assertEquals("Sam Lee", textValuesCaptor.getValue().get("teacherName"));
		assertEquals("EMP-1", textValuesCaptor.getValue().get("employeeCode"));
	}
}
