package com.altafjava.school.domain.health.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.health.model.HealthRecordCorrection;

public interface HealthRecordCorrectionRepository extends JpaRepository<HealthRecordCorrection, Long> {

	@Query("SELECT c FROM HealthRecordCorrection c WHERE c.tenantId = :tenantId AND c.healthRecordId = :healthRecordId "
			+ "ORDER BY c.createdAt DESC")
	Page<HealthRecordCorrection> findByHealthRecordIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("healthRecordId") Long healthRecordId, Pageable pageable);
}
