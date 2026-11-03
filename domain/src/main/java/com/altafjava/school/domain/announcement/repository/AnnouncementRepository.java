package com.altafjava.school.domain.announcement.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.model.AnnouncementStatus;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

	Optional<Announcement> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Announcement> findByIdAndTenantId(Long id, Long tenantId);

	// Serialises the dispatchers: the publish event and the lifecycle job can reach one announcement together.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Announcement a WHERE a.id = :id AND a.tenantId = :tenantId")
	Optional<Announcement> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

	@Query("""
			SELECT a FROM Announcement a
			WHERE a.tenantId = :tenantId
			  AND (:everyAuthor = true OR a.createdByUserId = :authorUserId)
			  AND (:#{#status == null} = TRUE OR a.status = :status)
			""")
	Page<Announcement> search(@Param("tenantId") Long tenantId, @Param("everyAuthor") boolean everyAuthor,
			@Param("authorUserId") Long authorUserId, @Param("status") AnnouncementStatus status, Pageable pageable);

	@Query("""
			SELECT a FROM Announcement a, AnnouncementRecipient r
			WHERE r.announcementId = a.id AND a.tenantId = :tenantId AND r.tenantId = :tenantId
			  AND r.userId = :userId
			  AND a.status = com.altafjava.school.domain.announcement.model.AnnouncementStatus.PUBLISHED
			  AND (a.expiresAt IS NULL OR a.expiresAt > :now)
			  AND (:unreadOnly = false OR r.readAt IS NULL)
			ORDER BY a.emergency DESC, a.publishedAt DESC
			""")
	Page<Announcement> findFeed(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
			@Param("now") Instant now, @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

	@Query("""
			SELECT a.id FROM Announcement a
			WHERE a.tenantId = :tenantId AND a.status = :status AND a.publishAt <= :now
			""")
	List<Long> findDueIds(@Param("tenantId") Long tenantId, @Param("status") AnnouncementStatus status,
			@Param("now") Instant now);

	@Query("""
			SELECT a FROM Announcement a
			WHERE a.tenantId = :tenantId
			  AND a.status = com.altafjava.school.domain.announcement.model.AnnouncementStatus.PUBLISHED
			  AND a.expiresAt <= :now
			""")
	List<Announcement> findExpired(@Param("tenantId") Long tenantId, @Param("now") Instant now);

	@Query("""
			SELECT a.id FROM Announcement a
			WHERE a.tenantId = :tenantId AND a.publishedAt IS NOT NULL AND a.dispatchedAt IS NULL
			""")
	List<Long> findUndispatchedIds(@Param("tenantId") Long tenantId);

	@Query("""
			SELECT a FROM Announcement a
			WHERE a.tenantId = :tenantId AND a.status IN :statuses AND a.updatedAt <= :cutoff
			  AND (a.expiresAt IS NULL OR a.expiresAt <= :cutoff)
			""")
	List<Announcement> findClosedBefore(@Param("tenantId") Long tenantId,
			@Param("statuses") Collection<AnnouncementStatus> statuses, @Param("cutoff") Instant cutoff);
}
