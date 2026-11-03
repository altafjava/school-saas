package com.altafjava.school.domain.announcement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.announcement.model.AnnouncementRecipient;

public interface AnnouncementRecipientRepository extends JpaRepository<AnnouncementRecipient, Long> {

	Optional<AnnouncementRecipient> findByAnnouncementIdAndUserIdAndTenantId(Long announcementId, Long userId,
			Long tenantId);

	Page<AnnouncementRecipient> findByAnnouncementIdAndTenantIdOrderByIdAsc(Long announcementId, Long tenantId,
			Pageable pageable);

	long countByAnnouncementIdAndTenantIdAndReadAtIsNotNull(Long announcementId, Long tenantId);

	List<AnnouncementRecipient> findTop200ByAnnouncementIdAndTenantIdAndNotifiedAtIsNullOrderByIdAsc(
			Long announcementId, Long tenantId);

	@Query("""
			SELECT COUNT(r) FROM AnnouncementRecipient r, Announcement a
			WHERE r.announcementId = a.id AND r.tenantId = :tenantId AND a.tenantId = :tenantId
			  AND r.userId = :userId AND r.readAt IS NULL
			  AND a.status = com.altafjava.school.domain.announcement.model.AnnouncementStatus.PUBLISHED
			  AND (a.expiresAt IS NULL OR a.expiresAt > :now)
			""")
	long countUnread(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
			@Param("now") java.time.Instant now);

	List<AnnouncementRecipient> findAllByUserIdAndTenantId(Long userId, Long tenantId);

	List<AnnouncementRecipient> findAllByUserIdAndTenantIdAndAnnouncementIdIn(Long userId, Long tenantId,
			java.util.Collection<Long> announcementIds);

	@Modifying
	@Query("DELETE FROM AnnouncementRecipient r WHERE r.tenantId = :tenantId AND r.userId = :userId")
	void deleteByUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

	@Modifying
	@Query("DELETE FROM AnnouncementRecipient r WHERE r.tenantId = :tenantId AND r.announcementId = :announcementId")
	void deleteByAnnouncement(@Param("tenantId") Long tenantId, @Param("announcementId") Long announcementId);
}
