package com.altafjava.school.domain.subject.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.subject.model.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

	// Blank q matches everything; pattern comes from LikePattern.contains.
	@Query("""
			SELECT s FROM Subject s
			WHERE s.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(s.code) LIKE :pattern ESCAPE '!' OR LOWER(s.name) LIKE :pattern ESCAPE '!')
			""")
	Page<Subject> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Page<Subject> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Subject> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Subject> findByIdAndTenantId(Long id, Long tenantId);

	List<Subject> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
