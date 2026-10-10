package com.altafjava.school.domain.leave.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.leave.model.LeaveType;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

	Page<LeaveType> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT l FROM LeaveType l
			WHERE l.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(l.name) LIKE :pattern ESCAPE '!')
			""")
	Page<LeaveType> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	List<LeaveType> findAllByTenantIdAndActiveTrue(Long tenantId);

	// Drives PayrollCalculator's loss-of-pay basis (see PayslipService) — active or not is
	// irrelevant here, a deactivated-but-still-referenced leave type keeps its paid/unpaid meaning.
	List<LeaveType> findAllByTenantIdAndPaidFalse(Long tenantId);

	Optional<LeaveType> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<LeaveType> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
