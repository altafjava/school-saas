package com.altafjava.school.domain.holiday.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.holiday.model.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

	Page<Holiday> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all). Dates are inclusive.
	@Query("""
			SELECT h FROM Holiday h
			WHERE h.tenantId = :tenantId
			  AND (:from IS NULL OR h.date >= :from)
			  AND (:to IS NULL OR h.date <= :to)
			  AND (:pattern IS NULL OR LOWER(h.name) LIKE :pattern ESCAPE '!')
			""")
	Page<Holiday> search(@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to,
			@Param("pattern") String pattern, Pageable pageable);

	List<Holiday> findAllByTenantId(Long tenantId);

	Optional<Holiday> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
