package com.altafjava.school.domain.curriculum.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.curriculum.model.Curriculum;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

	Page<Curriculum> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT c FROM Curriculum c
			WHERE c.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(c.name) LIKE :pattern ESCAPE '!'
			       OR LOWER(c.code) LIKE :pattern ESCAPE '!')
			""")
	Page<Curriculum> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Optional<Curriculum> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Curriculum> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
