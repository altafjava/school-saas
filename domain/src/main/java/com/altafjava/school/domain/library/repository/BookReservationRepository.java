package com.altafjava.school.domain.library.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.library.model.BookReservation;
import com.altafjava.school.domain.library.model.ReservationStatus;

public interface BookReservationRepository extends JpaRepository<BookReservation, Long> {

	Optional<BookReservation> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// Every filter is optional (null matches all).
	@Query("""
			SELECT r FROM BookReservation r
			WHERE r.tenantId = :tenantId
			  AND (:bookId IS NULL OR r.bookId = :bookId)
			  AND (:studentId IS NULL OR r.studentId = :studentId)
			  AND (:status IS NULL OR r.status = :status)
			""")
	Page<BookReservation> search(@Param("tenantId") Long tenantId, @Param("bookId") Long bookId,
			@Param("studentId") Long studentId, @Param("status") ReservationStatus status, Pageable pageable);

	// Oldest first, so the longest-waiting member is served first.
	Optional<BookReservation> findFirstByBookIdAndStatusAndTenantIdOrderByReservedAtAscIdAsc(Long bookId,
			ReservationStatus status, Long tenantId);

	Optional<BookReservation> findByHeldCopyIdAndStatusAndTenantId(Long heldCopyId, ReservationStatus status,
			Long tenantId);

	@Query("SELECT COUNT(r) > 0 FROM BookReservation r WHERE r.tenantId = :tenantId AND r.bookId = :bookId "
			+ "AND r.studentId = :studentId AND r.status IN (com.altafjava.school.domain.library.model.ReservationStatus.QUEUED, "
			+ "com.altafjava.school.domain.library.model.ReservationStatus.READY)")
	boolean existsLiveFor(@Param("tenantId") Long tenantId, @Param("bookId") Long bookId,
			@Param("studentId") Long studentId);

	@Query("SELECT COUNT(r) > 0 FROM BookReservation r WHERE r.tenantId = :tenantId AND r.bookId = :bookId "
			+ "AND r.status = com.altafjava.school.domain.library.model.ReservationStatus.QUEUED "
			+ "AND r.studentId <> :studentId")
	boolean existsQueuedForOthers(@Param("tenantId") Long tenantId, @Param("bookId") Long bookId,
			@Param("studentId") Long studentId);

	@Query("SELECT COUNT(r) FROM BookReservation r WHERE r.tenantId = :tenantId AND r.bookId = :bookId "
			+ "AND r.status = com.altafjava.school.domain.library.model.ReservationStatus.QUEUED "
			+ "AND r.reservedAt < :reservedAt")
	long countQueuedAhead(@Param("tenantId") Long tenantId, @Param("bookId") Long bookId,
			@Param("reservedAt") java.time.Instant reservedAt);

	@Query("SELECT r FROM BookReservation r WHERE r.tenantId = :tenantId "
			+ "AND r.status = com.altafjava.school.domain.library.model.ReservationStatus.READY "
			+ "AND r.holdExpiresOn < :today")
	List<BookReservation> findHoldsLapsedBefore(@Param("tenantId") Long tenantId, @Param("today") LocalDate today);
}
