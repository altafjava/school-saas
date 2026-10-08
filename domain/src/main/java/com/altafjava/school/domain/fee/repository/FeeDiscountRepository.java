package com.altafjava.school.domain.fee.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeeDiscount;

public interface FeeDiscountRepository extends JpaRepository<FeeDiscount, Long> {

	Optional<FeeDiscount> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT d FROM FeeDiscount d WHERE d.tenantId = :tenantId AND d.studentId = :studentId ORDER BY d.createdAt DESC")
	List<FeeDiscount> findByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);

	@Query("SELECT d FROM FeeDiscount d WHERE d.tenantId = :tenantId AND d.studentId = :studentId "
			+ "AND d.feeStructureId = :feeStructureId AND d.revokedAt IS NULL")
	List<FeeDiscount> findActiveByStudentAndStructure(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId, @Param("feeStructureId") Long feeStructureId);

	@Query("SELECT d FROM FeeDiscount d WHERE d.tenantId = :tenantId AND d.studentId IN :studentIds "
			+ "AND d.revokedAt IS NULL")
	List<FeeDiscount> findActiveByStudentIdIn(@Param("tenantId") Long tenantId,
			@Param("studentIds") List<Long> studentIds);
}
