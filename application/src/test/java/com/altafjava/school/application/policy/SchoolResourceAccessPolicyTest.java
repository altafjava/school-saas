package com.altafjava.school.application.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.application.security.OwnStudentResolver;
import com.altafjava.school.application.security.TeachingAssignmentResolver;
import com.altafjava.school.application.security.TeachingAssignments;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class SchoolResourceAccessPolicyTest {

	private static final Long TENANT_ID = 1L;
	private static final Long USER_ID = 9L;
	private static final Long CLASSROOM_ID = 40L;
	private static final Long STUDENT_ID = 30L;

	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TeachingAssignmentResolver teachingAssignmentResolver;
	@Mock
	private OwnStudentResolver ownStudentResolver;
	@InjectMocks
	private SchoolResourceAccessPolicy policy;

	private boolean readsClassroom(TeachingAssignments teaching) {
		UUID classroomPublicId = UUID.randomUUID();
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 1L, "2025-26", 20L);
		classroom.setId(CLASSROOM_ID);
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, TENANT_ID))
				.thenReturn(Optional.of(classroom));
		when(teachingAssignmentResolver.forUser(USER_ID, TENANT_ID)).thenReturn(teaching);
		return policy.isAllowed("9", TENANT_ID, ResourceType.CLASSROOM.name(), classroomPublicId.toString(),
				ResourceAction.READ.name());
	}

	private boolean readsStudent(Set<Long> ownStudentIds) {
		UUID studentPublicId = UUID.randomUUID();
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(STUDENT_ID);
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, TENANT_ID))
				.thenReturn(Optional.of(student));
		when(ownStudentResolver.forUser(USER_ID, TENANT_ID)).thenReturn(ownStudentIds);
		return policy.isAllowed("9", TENANT_ID, ResourceType.STUDENT.name(), studentPublicId.toString(),
				ResourceAction.READ.name());
	}

	@Test
	void isAllowed_classroomRead_classTeacher_allowed() {
		assertTrue(readsClassroom(new TeachingAssignments(20L, Set.of(CLASSROOM_ID), Map.of())));
	}

	@Test
	void isAllowed_classroomRead_timetabledSubjectTeacher_allowed() {
		assertTrue(readsClassroom(new TeachingAssignments(20L, Set.of(), Map.of(CLASSROOM_ID, Set.of(100L)))));
	}

	@Test
	void isAllowed_classroomRead_teacherOfAnotherClassroom_denied() {
		assertFalse(readsClassroom(new TeachingAssignments(20L, Set.of(99L), Map.of())));
	}

	@Test
	void isAllowed_classroomRead_callerWithoutTeacherRecord_denied() {
		assertFalse(readsClassroom(TeachingAssignments.NONE));
	}

	@Test
	void isAllowed_classroomRead_unknownOrMalformedClassroom_denied() {
		assertFalse(policy.isAllowed("9", TENANT_ID, ResourceType.CLASSROOM.name(), "not-a-uuid",
				ResourceAction.READ.name()));
	}

	@Test
	void isAllowed_studentRead_ownStudent_allowed() {
		assertTrue(readsStudent(Set.of(STUDENT_ID)));
	}

	@Test
	void isAllowed_studentRead_anotherStudent_denied() {
		assertFalse(readsStudent(Set.of(77L)));
	}

	@Test
	void isAllowed_nonReadAction_isLeftToPlatformRbac() {
		assertTrue(policy.isAllowed("9", TENANT_ID, ResourceType.STUDENT.name(), UUID.randomUUID().toString(),
				"WRITE"));
	}
}
