package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.altafjava.school.domain.curriculum.model.Board;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.model.GradingScale;
import com.altafjava.school.domain.curriculum.repository.BoardRepository;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleRepository;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {

	@Mock
	private CurriculumRepository curriculumRepository;
	@Mock
	private BoardRepository boardRepository;
	@Mock
	private GradingScaleRepository gradingScaleRepository;

	private CurriculumService curriculumService;

	@BeforeEach
	void setUp() {
		curriculumService = new CurriculumService(curriculumRepository, boardRepository, gradingScaleRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withExistingBoardAndNewCode_succeeds() {
		UUID boardPublicId = UUID.randomUUID();
		Board board = Board.create("CBSE", "CBSE", null);
		board.setId(1L);
		when(boardRepository.findByPublicIdAndTenantId(boardPublicId, 1L)).thenReturn(Optional.of(board));
		when(curriculumRepository.existsByCodeAndTenantId("CBSE-P", 1L)).thenReturn(false);
		when(curriculumRepository.save(any(Curriculum.class))).thenAnswer(inv -> inv.getArgument(0));

		Curriculum curriculum = curriculumService.create(boardPublicId.toString(), "CBSE Primary", "CBSE-P", null);

		assertEquals(1L, curriculum.getBoardId());
	}

	@Test
	void create_withNonExistentBoard_throwsResourceNotFound() {
		UUID boardPublicId = UUID.randomUUID();
		when(boardRepository.findByPublicIdAndTenantId(boardPublicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> curriculumService.create(boardPublicId.toString(), "CBSE Primary", "CBSE-P", null));
	}

	@Test
	void create_withDuplicateCode_throwsBusinessException() {
		UUID boardPublicId = UUID.randomUUID();
		Board board = Board.create("CBSE", "CBSE", null);
		board.setId(1L);
		when(boardRepository.findByPublicIdAndTenantId(boardPublicId, 1L)).thenReturn(Optional.of(board));
		when(curriculumRepository.existsByCodeAndTenantId("CBSE-P", 1L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> curriculumService.create(boardPublicId.toString(), "CBSE Primary", "CBSE-P", null));
	}

	@Test
	void assignGradingScale_resolvesScaleAndAssignsId() {
		UUID curriculumPublicId = UUID.randomUUID();
		UUID gradingScalePublicId = UUID.randomUUID();
		Curriculum curriculum = Curriculum.create(1L, "CBSE Primary", "CBSE-P", null);
		GradingScale scale = GradingScale.create("IB Scale", false);
		scale.setId(7L);
		when(curriculumRepository.findByPublicIdAndTenantId(curriculumPublicId, 1L))
				.thenReturn(Optional.of(curriculum));
		when(gradingScaleRepository.findByPublicIdAndTenantId(gradingScalePublicId, 1L))
				.thenReturn(Optional.of(scale));
		when(curriculumRepository.save(any(Curriculum.class))).thenAnswer(inv -> inv.getArgument(0));

		Curriculum updated = assertDoesNotThrow(() -> curriculumService.assignGradingScale(
				curriculumPublicId.toString(), gradingScalePublicId.toString(), ExpectedVersion.any()));

		assertEquals(7L, updated.getGradingScaleId());
	}
}
