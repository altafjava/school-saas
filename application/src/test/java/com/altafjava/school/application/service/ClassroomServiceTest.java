package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

	private static final UUID ACADEMIC_YEAR_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private CurriculumRepository curriculumRepository;
	@Mock
	private EventPublisher eventPublisher;

	private ClassroomService classroomService;

	@BeforeEach
	void setUp() {
		classroomService = new ClassroomService(classroomRepository, teacherRepository, academicYearRepository,
				studentClassroomLinkRepository, studentRepository, curriculumRepository, eventPublisher);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		AcademicYear academicYear = AcademicYear.create("2024-25", LocalDate.of(2024, 6, 1),
				LocalDate.of(2025, 5, 31), true);
		academicYear.setId(10L);
		lenient().when(academicYearRepository.findByPublicIdAndTenantId(ACADEMIC_YEAR_PUBLIC_ID, 1L))
				.thenReturn(java.util.Optional.of(academicYear));
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNonExistentClassTeacherId_throwsResourceNotFound() {
		when(teacherRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> classroomService.create("CLS-001", "Grade 5", "A", ACADEMIC_YEAR_PUBLIC_ID.toString(), 99L));

		verify(classroomRepository, never()).save(any());
	}

	@Test
	void create_withExistingClassTeacherId_succeeds() {
		when(teacherRepository.existsByIdAndTenantId(5L, 1L)).thenReturn(true);
		when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(
				() -> classroomService.create("CLS-001", "Grade 5", "A", ACADEMIC_YEAR_PUBLIC_ID.toString(), 5L));
	}

	@Test
	void create_withNullClassTeacherId_skipsValidation_succeeds() {
		when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(
				() -> classroomService.create("CLS-001", "Grade 5", "A", ACADEMIC_YEAR_PUBLIC_ID.toString(), null));

		verify(teacherRepository, never()).existsByIdAndTenantId(any(), any());
	}

	private Classroom classroomWithPublicId(UUID publicId, long id) {
		Classroom classroom = Classroom.create("CLS-001", "Grade 5", "A", 10L, "2024-25", null);
		classroom.setId(id);
		classroom.setPublicId(publicId);
		return classroom;
	}

	@Test
	void reassignTeacher_toExistingTeacher_succeeds() {
		UUID classroomPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(teacherRepository.existsByIdAndTenantId(5L, 1L)).thenReturn(true);
		when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

		Classroom updated = classroomService.reassignTeacher(classroomPublicId.toString(), 5L);

		assertEquals(5L, updated.getClassTeacherId());
	}

	@Test
	void reassignTeacher_toNonExistentTeacher_throwsResourceNotFound() {
		UUID classroomPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(teacherRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> classroomService.reassignTeacher(classroomPublicId.toString(), 99L));
	}

	@Test
	void assignCurriculum_toExistingCurriculum_succeeds() {
		UUID classroomPublicId = UUID.randomUUID();
		UUID curriculumPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		com.altafjava.school.domain.curriculum.model.Curriculum curriculum = com.altafjava.school.domain.curriculum.model.Curriculum
				.create(1L, "CBSE Primary", "CBSE-P", null);
		curriculum.setId(7L);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(curriculumRepository.findByPublicIdAndTenantId(curriculumPublicId, 1L))
				.thenReturn(Optional.of(curriculum));
		when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

		Classroom updated = classroomService.assignCurriculum(classroomPublicId.toString(),
				curriculumPublicId.toString());

		assertEquals(7L, updated.getCurriculumId());
	}

	@Test
	void moveToAcademicYear_toExistingYear_succeeds() {
		UUID classroomPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

		Classroom updated = classroomService.moveToAcademicYear(classroomPublicId.toString(),
				ACADEMIC_YEAR_PUBLIC_ID.toString());

		assertEquals(10L, updated.getAcademicYearId());
	}

	@Test
	void enrollStudent_notAlreadyEnrolledThisYear_succeeds() {
		UUID classroomPublicId = UUID.randomUUID();
		UUID studentPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test",
				LocalDate.of(2010, 1, 1));
		student.setId(3L);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.existsByStudentIdAndAcademicYearIdAndTenantId(3L, 10L, 1L))
				.thenReturn(false);
		when(studentClassroomLinkRepository.save(any(StudentClassroomLink.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		StudentClassroomLink link = classroomService.enrollStudent(classroomPublicId.toString(),
				studentPublicId.toString(), ACADEMIC_YEAR_PUBLIC_ID.toString());

		assertEquals(3L, link.getStudentId());
		assertEquals(1L, link.getClassroomId());
		verify(eventPublisher).publish(any());
	}

	@Test
	void enrollStudent_alreadyEnrolledThisYear_throwsBusinessException() {
		UUID classroomPublicId = UUID.randomUUID();
		UUID studentPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test",
				LocalDate.of(2010, 1, 1));
		student.setId(3L);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.existsByStudentIdAndAcademicYearIdAndTenantId(3L, 10L, 1L))
				.thenReturn(true);

		assertThrows(BusinessException.class, () -> classroomService.enrollStudent(classroomPublicId.toString(),
				studentPublicId.toString(), ACADEMIC_YEAR_PUBLIC_ID.toString()));

		verify(studentClassroomLinkRepository, never()).save(any());
	}

	@Test
	void withdrawStudentFromClassroom_enrolled_softDeletesLink() {
		UUID classroomPublicId = UUID.randomUUID();
		UUID studentPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test",
				LocalDate.of(2010, 1, 1));
		student.setId(3L);
		StudentClassroomLink link = StudentClassroomLink.create(3L, 1L, 10L, LocalDate.now());
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 3L, 1L))
				.thenReturn(Optional.of(link));
		when(studentClassroomLinkRepository.save(any(StudentClassroomLink.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		classroomService.withdrawStudentFromClassroom(classroomPublicId.toString(), studentPublicId.toString());

		assertEquals(true, link.isDeleted());
	}

	@Test
	void withdrawStudentFromClassroom_notEnrolled_throwsResourceNotFound() {
		UUID classroomPublicId = UUID.randomUUID();
		UUID studentPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test",
				LocalDate.of(2010, 1, 1));
		student.setId(3L);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 3L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> classroomService.withdrawStudentFromClassroom(classroomPublicId.toString(),
						studentPublicId.toString()));
	}

	@Test
	void assignRollNumber_enrolled_setsRollNumberOnLink() {
		UUID classroomPublicId = UUID.randomUUID();
		UUID studentPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(3L);
		StudentClassroomLink link = StudentClassroomLink.create(3L, 1L, 10L, LocalDate.now());
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 3L, 1L))
				.thenReturn(Optional.of(link));
		when(studentClassroomLinkRepository.save(any(StudentClassroomLink.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		StudentClassroomLink result = classroomService.assignRollNumber(classroomPublicId.toString(),
				studentPublicId.toString(), "12");

		assertEquals("12", result.getRollNumber());
	}

	@Test
	void assignRollNumber_notEnrolled_throwsResourceNotFound() {
		UUID classroomPublicId = UUID.randomUUID();
		UUID studentPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(3L);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 3L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> classroomService.assignRollNumber(
				classroomPublicId.toString(), studentPublicId.toString(), "12"));
	}

	@Test
	void listRoster_returnsEnrolledStudents() {
		UUID classroomPublicId = UUID.randomUUID();
		Classroom classroom = classroomWithPublicId(classroomPublicId, 1L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test",
				LocalDate.of(2010, 1, 1));
		student.setId(3L);
		StudentClassroomLink link = StudentClassroomLink.create(3L, 1L, 10L, LocalDate.now());
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		Page<StudentClassroomLink> linkPage = new PageImpl<>(java.util.List.of(link));
		when(studentClassroomLinkRepository.findByClassroomId(1L, 1L, PageRequest.of(0, 20)))
				.thenReturn(linkPage);
		when(studentRepository.findByIdAndTenantId(3L, 1L)).thenReturn(Optional.of(student));

		Page<Student> roster = classroomService.listRoster(classroomPublicId.toString(), PageRequest.of(0, 20));

		assertEquals(1, roster.getTotalElements());
		assertEquals("Alice", roster.getContent().get(0).getFirstName());
	}
}
