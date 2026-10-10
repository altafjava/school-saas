package com.altafjava.school.domain.certificate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.certificate.model.CertificateType;

public interface CertificateTypeRepository extends JpaRepository<CertificateType, Long> {

	Page<CertificateType> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT c FROM CertificateType c
			WHERE c.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(c.name) LIKE :pattern ESCAPE '!'
			       OR LOWER(c.code) LIKE :pattern ESCAPE '!')
			""")
	Page<CertificateType> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	List<CertificateType> findAllByTenantIdAndActiveTrue(Long tenantId);

	Optional<CertificateType> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
