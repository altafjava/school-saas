package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.counseling.repository.CounselingSessionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class CounselingSessionServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID TEACHER_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private CounselingSessionRepository counselingSessionRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TeacherRepository teacherRepository;
	private final ActivityLogService activityLogService = new NoOpActivityLogService();

	private CounselingSessionService counselingSessionService;

	@BeforeEach
	void setUp() {
		counselingSessionService = new CounselingSessionService(counselingSessionRepository, studentRepository,
				teacherRepository, activityLogService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private Teacher teacherWithId(long id) {
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(id);
		return teacher;
	}

	@Test
	void schedule_resolvesStudentAndCounselorByPublicId() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(teacherRepository.findByPublicIdAndTenantId(TEACHER_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(teacherWithId(20L)));
		when(counselingSessionRepository.save(any(CounselingSession.class))).thenAnswer(inv -> inv.getArgument(0));

		CounselingSession session = assertDoesNotThrow(() -> counselingSessionService.schedule(
				STUDENT_PUBLIC_ID.toString(), TEACHER_PUBLIC_ID.toString(), LocalDate.of(2026, 5, 1),
				"Discussed exam anxiety", true));

		assertEquals(10L, session.getStudentId());
		assertEquals(20L, session.getCounselorTeacherId());
	}

	@Test
	void schedule_unknownStudent_throwsResourceNotFoundException() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> counselingSessionService.schedule(
				STUDENT_PUBLIC_ID.toString(), TEACHER_PUBLIC_ID.toString(), LocalDate.of(2026, 5, 1), "Notes", true));
	}

	@Test
	void updateNotes_changesNotesAndFollowUpFlag() {
		UUID publicId = UUID.randomUUID();
		CounselingSession session = CounselingSession.schedule(10L, 20L, LocalDate.of(2026, 5, 1), "Initial", true);
		when(counselingSessionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(session));
		when(counselingSessionRepository.save(any(CounselingSession.class))).thenAnswer(inv -> inv.getArgument(0));

		CounselingSession updated = counselingSessionService.updateNotes(publicId.toString(), "Resolved", false);

		assertEquals("Resolved", updated.getNotes());
	}

	@Test
	void get_unknownPublicId_throwsResourceNotFoundException() {
		UUID publicId = UUID.randomUUID();
		when(counselingSessionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> counselingSessionService.get(publicId.toString()));
	}
}
