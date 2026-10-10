package com.altafjava.school.domain.library.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.library.model.Circulation;

public interface CirculationRepository extends JpaRepository<Circulation, Long> {

	// Every filter is optional (null matches all); {@code returned} true keeps handed-back loans, false those still
	// out.
	@Query("""
			SELECT c FROM Circulation c
			WHERE c.tenantId = :tenantId
			  AND (:studentId IS NULL OR c.studentId = :studentId)
			  AND (:bookId IS NULL OR EXISTS (SELECT 1 FROM BookCopy bc
			                                  WHERE bc.id = c.bookCopyId AND bc.tenantId = :tenantId
			                                    AND bc.bookId = :bookId))
			  AND (:returned IS NULL OR (:returned = true AND c.returnedAt IS NOT NULL)
			       OR (:returned = false AND c.returnedAt IS NULL))
			  AND (:from IS NULL OR c.checkedOutAt >= :from)
			  AND (:to IS NULL OR c.checkedOutAt <= :to)
			""")
	Page<Circulation> search(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("bookId") Long bookId, @Param("returned") Boolean returned, @Param("from") LocalDate from,
			@Param("to") LocalDate to, Pageable pageable);

	Optional<Circulation> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Circulation> findByBookCopyIdAndTenantIdAndReturnedAtIsNull(Long bookCopyId, Long tenantId);

	List<Circulation> findAllByTenantIdAndReturnedAtIsNull(Long tenantId);

	// Whether the member already has a copy of this title out.
	@Query("SELECT COUNT(c) > 0 FROM Circulation c, BookCopy bc WHERE c.bookCopyId = bc.id AND c.tenantId = :tenantId "
			+ "AND bc.tenantId = :tenantId AND bc.bookId = :bookId AND c.studentId = :studentId AND c.returnedAt IS NULL")
	boolean hasTitleOut(@Param("tenantId") Long tenantId, @Param("bookId") Long bookId,
			@Param("studentId") Long studentId);

	@Query("SELECT COALESCE(SUM(c.fineAmount), 0) FROM Circulation c WHERE c.tenantId = :tenantId AND c.fineAmount IS NOT NULL")
	BigDecimal sumFineAmountByTenantId(@Param("tenantId") Long tenantId);
}
