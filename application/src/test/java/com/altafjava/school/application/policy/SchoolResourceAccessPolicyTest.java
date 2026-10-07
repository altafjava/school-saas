package com.altafjava.school.application.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class SchoolResourceAccessPolicyTest {

	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private StudentGuardianLinkRepository studentGuardianLinkRepository;
	@Mock
	private TeacherRepository teacherRepository;

	private SchoolResourceAccessPolicy policy;

	private void setUp() {
		policy = new SchoolResourceAccessPolicy(classroomRepository, studentRepository, guardianRepository,
				studentGuardianLinkRepository, teacherRepository);
	}

	@Test
	void isAllowed_classroomRead_teacherAssignedToClassroom_allowed() {
		setUp();
		UUID classroomPublicId = UUID.randomUUID();
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(20L);
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 1L, "2025-26", 20L);
		when(teacherRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.of(teacher));
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));

		boolean allowed = policy.isAllowed("9", 1L, ResourceType.CLASSROOM.name(), classroomPublicId.toString(),
				ResourceAction.READ.name());

		assertTrue(allowed);
	}

	@Test
	void isAllowed_classroomRead_teacherAssignedToDifferentClassroom_denied() {
		setUp();
		UUID classroomPublicId = UUID.randomUUID();
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(20L);
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 1L, "2025-26", 99L);
		when(teacherRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.of(teacher));
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));

		boolean allowed = policy.isAllowed("9", 1L, ResourceType.CLASSROOM.name(), classroomPublicId.toString(),
				ResourceAction.READ.name());

		assertFalse(allowed);
	}

	@Test
	void isAllowed_classroomRead_callerHasNoLinkedTeacherRecord_denied() {
		setUp();
		UUID classroomPublicId = UUID.randomUUID();
		when(teacherRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.empty());

		boolean allowed = policy.isAllowed("9", 1L, ResourceType.CLASSROOM.name(), classroomPublicId.toString(),
				ResourceAction.READ.name());

		assertFalse(allowed);
	}

	private boolean guardianReadsStudent(StudentGuardianLink link) {
		setUp();
		UUID studentPublicId = UUID.randomUUID();
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(30L);
		Guardian guardian = Guardian.create("Jane", "Doe", "jane@school.test", "+14155552671", 9L);
		guardian.setId(10L);
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.of(guardian));
		when(studentGuardianLinkRepository.findByGuardianIdAndStudentIdAndTenantId(10L, 30L, 1L))
				.thenReturn(Optional.ofNullable(link));
		return policy.isAllowed("9", 1L, ResourceType.STUDENT.name(), studentPublicId.toString(),
				ResourceAction.READ.name());
	}

	@Test
	void isAllowed_studentRead_linkedGuardian_allowed() {
		assertTrue(guardianReadsStudent(StudentGuardianLink.create(30L, 10L, RelationshipType.MOTHER, true)));
	}

	@Test
	void isAllowed_studentRead_custodyRestrictedGuardian_denied() {
		StudentGuardianLink link = StudentGuardianLink.create(30L, 10L, RelationshipType.FATHER, false);
		link.restrictCustody("court order");

		assertFalse(guardianReadsStudent(link));
	}

	@Test
	void isAllowed_studentRead_unlinkedGuardian_denied() {
		assertFalse(guardianReadsStudent(null));
	}
}
