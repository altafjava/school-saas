package com.altafjava.school.domain.term.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.term.model.Term;

public interface TermRepository extends JpaRepository<Term, Long> {

	Page<Term> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all).
	@Query("""
			SELECT t FROM Term t
			WHERE t.tenantId = :tenantId
			  AND (:academicYearId IS NULL OR t.academicYearId = :academicYearId)
			  AND (:pattern IS NULL OR LOWER(t.name) LIKE :pattern ESCAPE '!')
			""")
	Page<Term> search(@Param("tenantId") Long tenantId, @Param("academicYearId") Long academicYearId,
			@Param("pattern") String pattern, Pageable pageable);

	Optional<Term> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByNameAndAcademicYearIdAndTenantId(String name, Long academicYearId, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	Optional<Term> findByIdAndTenantId(Long id, Long tenantId);

	// Mirrors AcademicYear's explicit current flag — "the current term" is whichever row
	// TermRolloverJob has flipped current=true for, not a live date-range computation.
	@Query("SELECT t FROM Term t WHERE t.tenantId = :tenantId AND t.current = true")
	Optional<Term> findCurrentByTenantId(@Param("tenantId") Long tenantId);

	// Used only by TermRolloverJob to detect when the current-flagged term needs to change.
	@Query("SELECT t FROM Term t WHERE t.tenantId = :tenantId AND :today BETWEEN t.startDate AND t.endDate")
	Optional<Term> findByDateRangeContainingAndTenantId(@Param("tenantId") Long tenantId,
			@Param("today") LocalDate today);
}
