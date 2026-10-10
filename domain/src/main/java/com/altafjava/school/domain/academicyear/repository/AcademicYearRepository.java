package com.altafjava.school.domain.academicyear.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.academicyear.model.AcademicYear;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

	Page<AcademicYear> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT a FROM AcademicYear a
			WHERE a.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(a.name) LIKE :pattern ESCAPE '!')
			""")
	Page<AcademicYear> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Optional<AcademicYear> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<AcademicYear> findByCurrentTrueAndTenantId(Long tenantId);

	// Tolerates a tenant that ended up with two current years: the one that started last wins.
	Optional<AcademicYear> findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(Long tenantId);

	boolean existsByTenantId(Long tenantId);

	Optional<AcademicYear> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	// The academic year immediately preceding one that starts on startDate — used to resolve leave
	// carry-forward source balances. Years are ordered by startDate, not an explicit link column.
	Optional<AcademicYear> findFirstByTenantIdAndStartDateBeforeOrderByStartDateDesc(Long tenantId,
			LocalDate startDate);
}
