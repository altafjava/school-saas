package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.repository.BoardRepository;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleRepository;
import com.altafjava.school.domain.error.SchoolErrorCode;

@Service
public class CurriculumService {

	/** Board/curriculum reference data changes rarely and is read on nearly every admission/roster path. */
	private final CurriculumRepository curriculumRepository;
	private final BoardRepository boardRepository;
	private final GradingScaleRepository gradingScaleRepository;

	public CurriculumService(CurriculumRepository curriculumRepository, BoardRepository boardRepository,
			GradingScaleRepository gradingScaleRepository) {
		this.curriculumRepository = curriculumRepository;
		this.boardRepository = boardRepository;
		this.gradingScaleRepository = gradingScaleRepository;
	}

	@Transactional(readOnly = true)
	public Page<Curriculum> list(String q, Pageable pageable) {
		return curriculumRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public Curriculum findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return curriculumRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Curriculum not found: " + publicId));
	}

	@Transactional
	public Curriculum create(String boardPublicId, String name, String code, String description) {
		Long tenantId = TenantContext.getCurrentTenantId();
		var board = boardRepository.findByPublicIdAndTenantId(UUID.fromString(boardPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Board not found: " + boardPublicId));
		if (curriculumRepository.existsByCodeAndTenantId(code, tenantId)) {
			throw new BusinessException(SchoolErrorCode.CODE_TAKEN, "Curriculum code already exists: " + code);
		}
		return curriculumRepository.save(Curriculum.create(board.getId(), name, code, description));
	}

	@Transactional
	public Curriculum updateDetails(String publicId, String name, String code, String description,
			ExpectedVersion expectedVersion) {
		Curriculum curriculum = findByPublicId(publicId);
		expectedVersion.verify(curriculum);
		curriculum.updateDetails(name, code, description);
		return curriculumRepository.save(curriculum);
	}

	// Also evicts GradingScaleService's resolved-thresholds cache: this changes what every
	// classroom under this curriculum resolves to, and that cache has no way to know which
	// classroom IDs are affected.
	@Transactional
	@CacheEvict(cacheNames = GradingScaleService.CACHE_GRADING_SCALE_THRESHOLDS, allEntries = true)
	public Curriculum assignGradingScale(String publicId, String gradingScalePublicId,
			ExpectedVersion expectedVersion) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Curriculum curriculum = findByPublicId(publicId);
		expectedVersion.verify(curriculum);
		var gradingScale = gradingScaleRepository
				.findByPublicIdAndTenantId(UUID.fromString(gradingScalePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Grading scale not found: " + gradingScalePublicId));
		curriculum.assignGradingScale(gradingScale.getId());
		return curriculumRepository.save(curriculum);
	}

	@Transactional
	public Curriculum deactivate(String publicId) {
		Curriculum curriculum = findByPublicId(publicId);
		curriculum.deactivate();
		return curriculumRepository.save(curriculum);
	}
}
