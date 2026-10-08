package com.altafjava.school.domain.visitor.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.visitor.model.VisitorRequest;
import com.altafjava.school.domain.visitor.model.VisitorRequestStatus;

public interface VisitorRequestRepository extends JpaRepository<VisitorRequest, Long> {

	Optional<VisitorRequest> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<VisitorRequest> findByIdAndTenantId(Long id, Long tenantId);

	// A null status matches every status.
	@Query("SELECT r FROM VisitorRequest r WHERE r.tenantId = :tenantId AND r.visitDate BETWEEN :from AND :to "
			+ "AND (:status IS NULL OR r.status = :status)")
	Page<VisitorRequest> search(@Param("tenantId") Long tenantId, @Param("from") LocalDate from,
			@Param("to") LocalDate to, @Param("status") VisitorRequestStatus status, Pageable pageable);

	@Query("SELECT r FROM VisitorRequest r WHERE r.tenantId = :tenantId AND r.hostEmployeeId = :hostEmployeeId "
			+ "AND r.visitDate BETWEEN :from AND :to AND (:status IS NULL OR r.status = :status)")
	Page<VisitorRequest> searchByHost(@Param("tenantId") Long tenantId,
			@Param("hostEmployeeId") Long hostEmployeeId, @Param("from") LocalDate from,
			@Param("to") LocalDate to, @Param("status") VisitorRequestStatus status, Pageable pageable);
}
