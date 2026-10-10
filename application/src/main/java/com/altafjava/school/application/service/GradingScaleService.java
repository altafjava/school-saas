package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.model.GradingScale;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleThresholdRepository;

/**
 * CRUD over tenant-defined, named grading scales and their thresholds, plus the resolution logic
 * ({@link #resolveEffectiveThresholds}) that {@code GradeService} and {@code StudentGpaService}
 * both depend on: a classroom's curriculum's grading scale, falling back to the tenant's one
 * {@code isDefault} scale when the classroom has no curriculum or the curriculum has no scale
 * assigned. Replaces the earlier single-JSON-blob-per-tenant design (see ROADMAP.md Phase 3.2) —
 * a real multi-board school needs more than one named scale.
 */
@Service
public class GradingScaleService {

	/**
	 * {@link #resolveEffectiveThresholds} is on the grade-entry/GPA hot path — cached, and evicted
	 * wholesale (not by classroom ID) since a scale/curriculum write doesn't know which classroom
	 * IDs are affected.
	 */
	public static final String CACHE_GRADING_SCALE_THRESHOLDS = "gradingScaleThresholds";

	private final GradingScaleRepository gradingScaleRepository;
	private final GradingScaleThresholdRepository gradingScaleThresholdRepository;
	private final ClassroomRepository classroomRepository;
	private final CurriculumRepository curriculumRepository;

	public GradingScaleService(GradingScaleRepository gradingScaleRepository,
			GradingScaleThresholdRepository gradingScaleThresholdRepository, ClassroomRepository classroomRepository,
			CurriculumRepository curriculumRepository) {
		this.gradingScaleRepository = gradingScaleRepository;
		this.gradingScaleThresholdRepository = gradingScaleThresholdRepository;
		this.classroomRepository = classroomRepository;
		this.curriculumRepository = curriculumRepository;
	}

	@Transactional(readOnly = true)
	public Page<GradingScale> list(Pageable pageable) {
		return gradingScaleRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public GradingScale findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return gradingScaleRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Grading scale not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public List<GradingScaleThreshold> listThresholds(String gradingScalePublicId) {
		GradingScale scale = findByPublicId(gradingScalePublicId);
		return gradingScaleThresholdRepository.findAllByGradingScaleIdAndTenantId(scale.getId(),
				TenantContext.getCurrentTenantId());
	}

	@Transactional
	@CacheEvict(cacheNames = CACHE_GRADING_SCALE_THRESHOLDS, allEntries = true)
	public GradingScale create(String name, List<GradingScaleThresholdInput> thresholds, boolean isDefault) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (gradingScaleRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new BusinessException("Grading scale already exists: " + name);
		}
		validateCoverage(thresholds);
		if (isDefault) {
			unmarkExistingDefault(tenantId);
		}
		GradingScale scale = gradingScaleRepository.save(GradingScale.create(name, isDefault));
		saveThresholds(scale.getId(), thresholds);
		return scale;
	}

	@Transactional
	@CacheEvict(cacheNames = CACHE_GRADING_SCALE_THRESHOLDS, allEntries = true)
	public GradingScale updateThresholds(String publicId, List<GradingScaleThresholdInput> thresholds,
			ExpectedVersion expectedVersion) {
		validateCoverage(thresholds);
		Long tenantId = TenantContext.getCurrentTenantId();
		GradingScale scale = gradingScaleRepository
				.findWithVersionIncrementByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Grading scale not found: " + publicId));
		expectedVersion.verify(scale);
		gradingScaleThresholdRepository.findAllByGradingScaleIdAndTenantId(scale.getId(), tenantId)
				.forEach(gradingScaleThresholdRepository::delete);
		saveThresholds(scale.getId(), thresholds);
		return scale;
	}

	@Transactional
	@CacheEvict(cacheNames = CACHE_GRADING_SCALE_THRESHOLDS, allEntries = true)
	public GradingScale markAsDefault(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		unmarkExistingDefault(tenantId);
		GradingScale scale = findByPublicId(publicId);
		scale.markAsDefault();
		return gradingScaleRepository.save(scale);
	}

	@Transactional
	@CacheEvict(cacheNames = CACHE_GRADING_SCALE_THRESHOLDS, allEntries = true)
	public GradingScale deactivate(String publicId) {
		GradingScale scale = findByPublicId(publicId);
		if (scale.isDefault()) {
			throw new BusinessException("Cannot deactivate the tenant's default grading scale");
		}
		scale.deactivate();
		return gradingScaleRepository.save(scale);
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CACHE_GRADING_SCALE_THRESHOLDS, keyGenerator = "tenantAwareCacheKeyGenerator")
	public List<GradingScaleThreshold> resolveEffectiveThresholds(Long classroomId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long gradingScaleId = resolveGradingScaleId(tenantId, classroomId);
		List<GradingScaleThreshold> thresholds = gradingScaleThresholdRepository
				.findAllByGradingScaleIdAndTenantId(gradingScaleId, tenantId);
		if (thresholds.isEmpty()) {
			throw new BusinessException("Grading scale " + gradingScaleId + " has no thresholds configured");
		}
		return thresholds;
	}

	private Long resolveGradingScaleId(Long tenantId, Long classroomId) {
		Long curriculumGradingScaleId = classroomRepository.findByIdAndTenantId(classroomId, tenantId)
				.map(Classroom::getCurriculumId)
				.flatMap(curriculumId -> curriculumRepository.findByIdAndTenantId(curriculumId, tenantId))
				.map(Curriculum::getGradingScaleId)
				.orElse(null);
		if (curriculumGradingScaleId != null) {
			return curriculumGradingScaleId;
		}
		return gradingScaleRepository.findByIsDefaultTrueAndTenantId(tenantId)
				.map(GradingScale::getId)
				.orElseThrow(() -> new BusinessException("No default grading scale configured for this tenant"));
	}

	private void unmarkExistingDefault(Long tenantId) {
		gradingScaleRepository.findByIsDefaultTrueAndTenantId(tenantId).ifPresent(existing -> {
			existing.unmarkAsDefault();
			gradingScaleRepository.save(existing);
		});
	}

	private void saveThresholds(Long gradingScaleId, List<GradingScaleThresholdInput> thresholds) {
		thresholds.forEach(input -> gradingScaleThresholdRepository.save(
				GradingScaleThreshold.create(gradingScaleId, input.letter(), input.minPercentage(), input.points())));
	}

	private void validateCoverage(List<GradingScaleThresholdInput> thresholds) {
		if (thresholds == null || thresholds.isEmpty()) {
			throw new BusinessException("Grading scale must contain at least one threshold");
		}
		BigDecimal lowestMinPercentage = thresholds.stream()
				.map(GradingScaleThresholdInput::minPercentage)
				.min(Comparator.naturalOrder())
				.orElseThrow();
		if (lowestMinPercentage.compareTo(BigDecimal.ZERO) > 0) {
			throw new BusinessException("Grading scale must cover down to 0%");
		}
	}
}
