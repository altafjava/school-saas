package com.altafjava.school.domain.importing.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.importing.model.ImportJobError;
import com.altafjava.school.domain.importing.model.ImportPhase;

public interface ImportJobErrorRepository extends JpaRepository<ImportJobError, Long> {

	@Query("SELECT e FROM ImportJobError e WHERE e.tenantId = :tenantId AND e.importJobId = :jobId "
			+ "AND e.phase = :phase ORDER BY e.lineNumber, e.id")
	Page<ImportJobError> findByJob(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId,
			@Param("phase") ImportPhase phase, Pageable pageable);

	@Query("SELECT e FROM ImportJobError e WHERE e.tenantId = :tenantId AND e.importJobId = :jobId "
			+ "AND e.phase = :phase ORDER BY e.lineNumber, e.id")
	List<ImportJobError> findAllByJob(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId,
			@Param("phase") ImportPhase phase);

	// A rerun replaces its phase's report rather than adding to it.
	@Modifying
	@Query("DELETE FROM ImportJobError e WHERE e.tenantId = :tenantId AND e.importJobId = :jobId AND e.phase = :phase")
	void deleteByJob(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId, @Param("phase") ImportPhase phase);
}
