package com.altafjava.school.domain.library.repository;

import java.math.BigDecimal;
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

	Page<Circulation> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

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
