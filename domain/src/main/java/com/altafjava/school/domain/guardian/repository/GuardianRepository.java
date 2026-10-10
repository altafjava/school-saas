package com.altafjava.school.domain.guardian.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.guardian.model.Guardian;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

	List<Guardian> findAllByIdInAndTenantId(Collection<Long> ids, Long tenantId);

	// Blank q matches everything; pattern comes from LikePattern.contains.
	@Query("""
			SELECT g FROM Guardian g
			WHERE g.tenantId = :tenantId
			  AND (:pattern IS NULL
			       OR LOWER(CONCAT(g.firstName, ' ', g.lastName)) LIKE :pattern ESCAPE '!'
			       OR LOWER(g.email) LIKE :pattern ESCAPE '!')
			""")
	Page<Guardian> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Page<Guardian> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Guardian> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Guardian> findByUserIdAndTenantId(Long userId, Long tenantId);

	// A "pending, unclaimed" guardian record — created by an admin/saga ahead of the guardian
	// ever logging in — that self-registration can claim by matching email.
	Optional<Guardian> findByEmailAndTenantIdAndUserIdIsNull(String email, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	Optional<Guardian> findByIdAndTenantId(Long id, Long tenantId);
}
