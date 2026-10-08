package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;
import com.altafjava.school.domain.lifecycle.model.LifecycleStage;
import com.altafjava.school.domain.lifecycle.model.LifecycleTransition;
import com.altafjava.school.domain.lifecycle.repository.LifecycleTransitionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class LifecycleTimelineServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private LifecycleTransitionRepository repository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private AdmissionRepository admissionRepository;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;

	private LifecycleTimelineService service;

	@BeforeEach
	void setUp() {
		service = new LifecycleTimelineService(repository, studentRepository, admissionRepository,
				studentDataAccessGuard);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clear() {
		TenantContext.ForTesting.clear();
	}

	private Student student() {
		Student student = Student.create("STU-1", "Alice", "Smith", "a@school.test", LocalDate.of(2010, 1, 1));
		student.setId(5L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		return student;
	}

	@Test
	void forStudent_returnsTheJoinedTimeline() {
		student();
		LifecycleTransition row = LifecycleTransition.record(null, 5L, null, LifecycleStage.ENROLLED, null, null, null);
		when(repository.findTimelineForStudent(1L, 5L)).thenReturn(List.of(row));

		assertEquals(List.of(row), service.forStudent(STUDENT_PUBLIC_ID.toString()));
	}

	@Test
	void forStudent_appliesTheOwnChildGuard_beforeReadingAnyHistory() {
		student();
		doThrow(new AccessDeniedException("no")).when(studentDataAccessGuard).assertCanView(1L,
				STUDENT_PUBLIC_ID.toString());

		assertThrows(AccessDeniedException.class, () -> service.forStudent(STUDENT_PUBLIC_ID.toString()));

		verify(repository, never()).findTimelineForStudent(1L, 5L);
	}

	@Test
	void forAdmission_unknown_throwsNotFound() {
		UUID id = UUID.randomUUID();
		when(admissionRepository.findByPublicIdAndTenantId(id, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.forAdmission(id.toString()));
	}
}
