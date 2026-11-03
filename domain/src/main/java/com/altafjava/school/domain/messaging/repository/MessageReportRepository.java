package com.altafjava.school.domain.messaging.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.messaging.model.MessageReport;
import com.altafjava.school.domain.messaging.model.MessageReportStatus;

public interface MessageReportRepository extends JpaRepository<MessageReport, Long> {

	Optional<MessageReport> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByMessageIdAndReportedByUserIdAndTenantId(Long messageId, Long reportedByUserId, Long tenantId);

	@Query("""
			SELECT r FROM MessageReport r
			WHERE r.tenantId = :tenantId AND (:#{#status == null} = TRUE OR r.status = :status)
			""")
	Page<MessageReport> search(@Param("tenantId") Long tenantId, @Param("status") MessageReportStatus status,
			Pageable pageable);

	List<MessageReport> findAllByReportedByUserIdAndTenantId(Long reportedByUserId, Long tenantId);

	List<MessageReport> findAllByConversationIdAndTenantId(Long conversationId, Long tenantId);
}
