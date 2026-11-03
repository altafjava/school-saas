package com.altafjava.school.domain.messaging.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.messaging.model.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	Optional<Conversation> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Conversation> findByGuardianIdAndTeacherIdAndStudentIdAndTenantId(Long guardianId, Long teacherId,
			Long studentId, Long tenantId);

	@Query("""
			SELECT c FROM Conversation c
			WHERE c.tenantId = :tenantId AND (c.guardianUserId = :userId OR c.teacherUserId = :userId)
			ORDER BY c.lastMessageAt DESC
			""")
	Page<Conversation> findForParticipant(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
			Pageable pageable);

	@Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId ORDER BY c.lastMessageAt DESC")
	Page<Conversation> findAllForReview(@Param("tenantId") Long tenantId, Pageable pageable);

	// A plain update, so two people replying at once never collide on the row's version.
	@Modifying
	@Query("UPDATE Conversation c SET c.lastMessageAt = :at WHERE c.id = :id AND c.tenantId = :tenantId")
	void touch(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("at") Instant at);

	List<Conversation> findAllByStudentIdAndTenantId(Long studentId, Long tenantId);

	@Query("""
			SELECT c FROM Conversation c
			WHERE c.tenantId = :tenantId AND (c.guardianUserId = :userId OR c.teacherUserId = :userId)
			""")
	List<Conversation> findAllForParticipant(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

	@Query("""
			SELECT c FROM Conversation c
			WHERE c.tenantId = :tenantId AND COALESCE(c.lastMessageAt, c.createdAt) <= :cutoff
			""")
	List<Conversation> findIdleSince(@Param("tenantId") Long tenantId, @Param("cutoff") Instant cutoff);
}
