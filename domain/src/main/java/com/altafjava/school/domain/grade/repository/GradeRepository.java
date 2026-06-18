package com.altafjava.school.domain.grade.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.grade.model.Grade;

public interface GradeRepository extends JpaRepository<Grade, Long> {

	Page<Grade> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Grade> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT g FROM Grade g WHERE g.tenantId = :tenantId AND g.studentId = :studentId")
	List<Grade> findByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);

	boolean existsByStudentIdAndExamIdAndTenantId(Long studentId, Long examId, Long tenantId);
}
