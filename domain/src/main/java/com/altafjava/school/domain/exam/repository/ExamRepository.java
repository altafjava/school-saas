package com.altafjava.school.domain.exam.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.exam.model.Exam;

public interface ExamRepository extends JpaRepository<Exam, Long> {

	Page<Exam> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Exam> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT e FROM Exam e WHERE e.tenantId = :tenantId AND e.scheduledAt BETWEEN :from AND :to")
	List<Exam> findUpcoming(@Param("tenantId") Long tenantId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);
}
