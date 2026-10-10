package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
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
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.model.GradingScale;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleThresholdRepository;

@ExtendWith(MockitoExtension.class)
class GradingScaleServiceTest {

	@Mock
	private GradingScaleRepository gradingScaleRepository;
	@Mock
	private GradingScaleThresholdRepository gradingScaleThresholdRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private CurriculumRepository curriculumRepository;

	private GradingScaleService gradingScaleService;

	@BeforeEach
	void setUp() {
		gradingScaleService = new GradingScaleService(gradingScaleRepository, gradingScaleThresholdRepository,
				classroomRepository, curriculumRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private List<GradingScaleThresholdInput> validThresholds() {
		return List.of(
				new GradingScaleThresholdInput("A", new BigDecimal("90"), new BigDecimal("4.0")),
				new GradingScaleThresholdInput("F", BigDecimal.ZERO, BigDecimal.ZERO));
	}

	@Test
	void create_withNewName_succeeds() {
		when(gradingScaleRepository.existsByNameAndTenantId("CBSE Scale", 1L)).thenReturn(false);
		when(gradingScaleRepository.save(any(GradingScale.class))).thenAnswer(inv -> inv.getArgument(0));

		GradingScale scale = gradingScaleService.create("CBSE Scale", validThresholds(), false);

		assertEquals("CBSE Scale", scale.getName());
		verify(gradingScaleThresholdRepository, times(2)).save(any(GradingScaleThreshold.class));
	}

	@Test
	void create_duplicateName_throwsBusinessException() {
		when(gradingScaleRepository.existsByNameAndTenantId("CBSE Scale", 1L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> gradingScaleService.create("CBSE Scale", validThresholds(), false));
	}

	@Test
	void create_thresholdsNotCoveringZero_throwsBusinessException() {
		List<GradingScaleThresholdInput> gapped = List.of(
				new GradingScaleThresholdInput("A", new BigDecimal("90"), new BigDecimal("4.0")));

		assertThrows(BusinessException.class, () -> gradingScaleService.create("Gapped", gapped, false));
		verify(gradingScaleRepository, never()).save(any());
	}

	@Test
	void create_asDefault_unmarksPreviousDefault() {
		GradingScale existingDefault = GradingScale.create("Old Default", true);
		existingDefault.setId(5L);
		when(gradingScaleRepository.existsByNameAndTenantId("New Default", 1L)).thenReturn(false);
		when(gradingScaleRepository.findByIsDefaultTrueAndTenantId(1L)).thenReturn(Optional.of(existingDefault));
		when(gradingScaleRepository.save(any(GradingScale.class))).thenAnswer(inv -> inv.getArgument(0));

		gradingScaleService.create("New Default", validThresholds(), true);

		assertEquals(false, existingDefault.isDefault());
		verify(gradingScaleRepository, times(2)).save(any(GradingScale.class));
	}

	@Test
	void updateThresholds_replacesExistingRows() {
		UUID publicId = UUID.randomUUID();
		GradingScale scale = GradingScale.create("Scale", false);
		scale.setId(9L);
		GradingScaleThreshold existing = GradingScaleThreshold.create(9L, "OLD", BigDecimal.ZERO, BigDecimal.ZERO);
		when(gradingScaleRepository.findWithVersionIncrementByPublicIdAndTenantId(publicId, 1L))
				.thenReturn(Optional.of(scale));
		when(gradingScaleThresholdRepository.findAllByGradingScaleIdAndTenantId(9L, 1L))
				.thenReturn(List.of(existing));

		gradingScaleService.updateThresholds(publicId.toString(), validThresholds(), ExpectedVersion.any());

		verify(gradingScaleThresholdRepository).delete(existing);
		verify(gradingScaleThresholdRepository, times(2)).save(any(GradingScaleThreshold.class));
	}

	@Test
	void deactivate_defaultScale_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		GradingScale scale = GradingScale.create("Default", true);
		when(gradingScaleRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(scale));

		assertThrows(BusinessException.class, () -> gradingScaleService.deactivate(publicId.toString()));
	}

	@Test
	void resolveEffectiveThresholds_classroomWithCurriculumScale_usesCurriculumScale() {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 1L, "2026-27", null);
		classroom.setId(20L);
		classroom.assignCurriculum(30L);
		Curriculum curriculum = Curriculum.create(1L, "CBSE Primary", "CBSE-P", null);
		curriculum.setId(30L);
		curriculum.assignGradingScale(40L);
		GradingScaleThreshold threshold = GradingScaleThreshold.create(40L, "A", new BigDecimal("90"),
				new BigDecimal("4.0"));
		when(classroomRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(classroom));
		when(curriculumRepository.findByIdAndTenantId(30L, 1L)).thenReturn(Optional.of(curriculum));
		when(gradingScaleThresholdRepository.findAllByGradingScaleIdAndTenantId(40L, 1L))
				.thenReturn(List.of(threshold));

		List<GradingScaleThreshold> resolved = gradingScaleService.resolveEffectiveThresholds(20L);

		assertEquals(1, resolved.size());
		verify(gradingScaleRepository, never()).findByIsDefaultTrueAndTenantId(any());
	}

	@Test
	void resolveEffectiveThresholds_classroomWithoutCurriculum_fallsBackToTenantDefault() {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 1L, "2026-27", null);
		classroom.setId(20L);
		GradingScale defaultScale = GradingScale.create("Default", true);
		defaultScale.setId(99L);
		GradingScaleThreshold threshold = GradingScaleThreshold.create(99L, "A", new BigDecimal("90"),
				new BigDecimal("4.0"));
		when(classroomRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(classroom));
		when(gradingScaleRepository.findByIsDefaultTrueAndTenantId(1L)).thenReturn(Optional.of(defaultScale));
		when(gradingScaleThresholdRepository.findAllByGradingScaleIdAndTenantId(99L, 1L))
				.thenReturn(List.of(threshold));

		List<GradingScaleThreshold> resolved = gradingScaleService.resolveEffectiveThresholds(20L);

		assertEquals(1, resolved.size());
	}

	@Test
	void resolveEffectiveThresholds_noDefaultConfigured_throwsBusinessException() {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 1L, "2026-27", null);
		classroom.setId(20L);
		when(classroomRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(classroom));
		when(gradingScaleRepository.findByIsDefaultTrueAndTenantId(1L)).thenReturn(Optional.empty());

		assertThrows(BusinessException.class, () -> gradingScaleService.resolveEffectiveThresholds(20L));
	}

	@Test
	void markAsDefault_unmarksPreviousAndMarksTarget() {
		UUID publicId = UUID.randomUUID();
		GradingScale previousDefault = GradingScale.create("Old", true);
		GradingScale target = GradingScale.create("New", false);
		when(gradingScaleRepository.findByIsDefaultTrueAndTenantId(1L)).thenReturn(Optional.of(previousDefault));
		when(gradingScaleRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(target));
		when(gradingScaleRepository.save(any(GradingScale.class))).thenAnswer(inv -> inv.getArgument(0));

		GradingScale updated = assertDoesNotThrow(() -> gradingScaleService.markAsDefault(publicId.toString()));

		assertEquals(false, previousDefault.isDefault());
		assertEquals(true, updated.isDefault());
	}
}
