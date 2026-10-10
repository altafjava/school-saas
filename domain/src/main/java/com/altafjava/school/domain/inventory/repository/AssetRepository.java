package com.altafjava.school.domain.inventory.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.inventory.model.AssetStatus;

public interface AssetRepository extends JpaRepository<Asset, Long> {

	Page<Asset> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all).
	@Query("""
			SELECT a FROM Asset a
			WHERE a.tenantId = :tenantId
			  AND (:status IS NULL OR a.status = :status)
			  AND (:pattern IS NULL OR LOWER(a.name) LIKE :pattern ESCAPE '!' OR LOWER(a.assetCode) LIKE :pattern ESCAPE '!' OR LOWER(a.category) LIKE :pattern ESCAPE '!')
			""")
	Page<Asset> search(@Param("tenantId") Long tenantId, @Param("status") AssetStatus status,
			@Param("pattern") String pattern, Pageable pageable);

	Optional<Asset> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Asset> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByAssetCodeAndTenantId(String assetCode, Long tenantId);
}
