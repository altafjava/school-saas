package com.altafjava.school.domain.curriculum.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.curriculum.model.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {

	Page<Board> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT b FROM Board b
			WHERE b.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(b.name) LIKE :pattern ESCAPE '!'
			       OR LOWER(b.code) LIKE :pattern ESCAPE '!')
			""")
	Page<Board> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Optional<Board> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);
}
