package com.altafjava.school.domain.fee.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeeStructure;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

	Page<FeeStructure> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT f FROM FeeStructure f
			WHERE f.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(f.name) LIKE :pattern ESCAPE '!')
			""")
	Page<FeeStructure> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	List<FeeStructure> findAllByTenantId(Long tenantId);

	List<FeeStructure> findAllByIdInAndTenantId(Collection<Long> ids, Long tenantId);

	Optional<FeeStructure> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);
}
