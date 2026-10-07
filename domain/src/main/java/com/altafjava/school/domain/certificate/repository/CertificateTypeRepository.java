package com.altafjava.school.domain.certificate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.certificate.model.CertificateType;

public interface CertificateTypeRepository extends JpaRepository<CertificateType, Long> {

	Page<CertificateType> findAllByTenantId(Long tenantId, Pageable pageable);

	List<CertificateType> findAllByTenantIdAndActiveTrue(Long tenantId);

	Optional<CertificateType> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
