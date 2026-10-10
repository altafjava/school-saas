package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private CurriculumRepository curriculumRepository;

	private SubjectService subjectService;

	@BeforeEach
	void setUp() {
		subjectService = new SubjectService(subjectRepository, curriculumRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNewCode_succeeds() {
		when(subjectRepository.existsByCodeAndTenantId("MATH", 1L)).thenReturn(false);
		when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

		Subject subject = assertDoesNotThrow(() -> subjectService.create("MATH", "Mathematics", "Core mathematics"));

		assertEquals("MATH", subject.getCode());
		assertEquals("Mathematics", subject.getName());
	}

	@Test
	void create_withDuplicateCode_throwsBusinessException() {
		when(subjectRepository.existsByCodeAndTenantId("MATH", 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> subjectService.create("MATH", "Mathematics", null));

		verify(subjectRepository, never()).save(any());
	}

	@Test
	void findByPublicId_withNonExistentId_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		when(subjectRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> subjectService.findByPublicId(publicId.toString()));
	}

	@Test
	void deactivate_withExistingSubject_marksInactive() {
		UUID publicId = UUID.randomUUID();
		Subject subject = Subject.create("MATH", "Mathematics", null);
		when(subjectRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(subject));
		when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

		Subject deactivated = subjectService.deactivate(publicId.toString());

		assertFalse(deactivated.isActive());
	}

	@Test
	void assignCurriculum_withExistingCurriculum_setsCurriculumId() {
		UUID subjectPublicId = UUID.randomUUID();
		UUID curriculumPublicId = UUID.randomUUID();
		Subject subject = Subject.create("MATH", "Mathematics", null);
		Curriculum curriculum = Curriculum.create(1L, "IB Diploma", "IB-DP", null);
		curriculum.setId(9L);
		when(subjectRepository.findByPublicIdAndTenantId(subjectPublicId, 1L)).thenReturn(Optional.of(subject));
		when(curriculumRepository.findByPublicIdAndTenantId(curriculumPublicId, 1L))
				.thenReturn(Optional.of(curriculum));
		when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

		Subject updated = subjectService.assignCurriculum(subjectPublicId.toString(), curriculumPublicId.toString(),
				ExpectedVersion.any());

		assertEquals(9L, updated.getCurriculumId());
	}

	@Test
	void assignCurriculum_withNonExistentCurriculum_throwsResourceNotFound() {
		UUID subjectPublicId = UUID.randomUUID();
		UUID curriculumPublicId = UUID.randomUUID();
		Subject subject = Subject.create("MATH", "Mathematics", null);
		when(subjectRepository.findByPublicIdAndTenantId(subjectPublicId, 1L)).thenReturn(Optional.of(subject));
		when(curriculumRepository.findByPublicIdAndTenantId(curriculumPublicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> subjectService.assignCurriculum(subjectPublicId.toString(), curriculumPublicId.toString(),
						ExpectedVersion.any()));

		verify(subjectRepository, never()).save(any());
	}
}
