package com.altafjava.school.domain.curriculum.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import com.altafjava.school.domain.curriculum.model.GradingScale;

public interface GradingScaleRepository extends JpaRepository<GradingScale, Long> {

	Page<GradingScale> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<GradingScale> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// For edits that change only the scale's thresholds: the scale's own version must still advance.
	@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
	Optional<GradingScale> findWithVersionIncrementByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<GradingScale> findByIdAndTenantId(Long id, Long tenantId);

	Optional<GradingScale> findByIsDefaultTrueAndTenantId(Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
