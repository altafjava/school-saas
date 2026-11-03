package com.altafjava.school.domain.importing.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.importing.model.ImportJob;
import com.altafjava.school.domain.importing.model.ImportJobStatus;
import com.altafjava.school.domain.importing.model.ImportType;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

	Optional<ImportJob> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// Every filter is optional (null matches all).
	@Query("""
			SELECT j FROM ImportJob j
			WHERE j.tenantId = :tenantId
			  AND (:#{#importType == null} = TRUE OR j.importType = :importType)
			  AND (:#{#status == null} = TRUE OR j.status = :status)
			""")
	Page<ImportJob> search(@Param("tenantId") Long tenantId, @Param("importType") ImportType importType,
			@Param("status") ImportJobStatus status, Pageable pageable);
}
