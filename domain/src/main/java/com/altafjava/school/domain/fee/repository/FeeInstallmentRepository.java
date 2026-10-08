package com.altafjava.school.domain.fee.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeeInstallment;

public interface FeeInstallmentRepository extends JpaRepository<FeeInstallment, Long> {

	@Query("SELECT i FROM FeeInstallment i WHERE i.tenantId = :tenantId AND i.studentId = :studentId "
			+ "ORDER BY i.feeStructureId, i.sequenceNumber")
	List<FeeInstallment> findByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);

	@Query("SELECT i FROM FeeInstallment i WHERE i.tenantId = :tenantId AND i.studentId = :studentId "
			+ "AND i.feeStructureId = :feeStructureId ORDER BY i.sequenceNumber")
	List<FeeInstallment> findByStudentAndStructure(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId, @Param("feeStructureId") Long feeStructureId);

	@Query("SELECT i FROM FeeInstallment i WHERE i.tenantId = :tenantId AND i.studentId IN :studentIds")
	List<FeeInstallment> findByStudentIdIn(@Param("tenantId") Long tenantId,
			@Param("studentIds") List<Long> studentIds);
}
