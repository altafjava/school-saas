package com.altafjava.school.domain.helpdesk.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.helpdesk.model.Ticket;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;
import com.altafjava.school.domain.helpdesk.model.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	@Query("SELECT t FROM Ticket t WHERE t.tenantId = :tenantId "
			+ "AND (:#{#status == null} = TRUE OR t.status = :status) "
			+ "AND (:#{#category == null} = TRUE OR t.category = :category) "
			+ "AND (:#{#assignedToUserId == null} = TRUE OR t.assignedToUserId = :assignedToUserId) "
			+ "AND (:#{#pattern == null} = TRUE OR LOWER(t.subject) LIKE :pattern ESCAPE '!')")
	Page<Ticket> search(@Param("tenantId") Long tenantId, @Param("status") TicketStatus status,
			@Param("category") TicketCategory category, @Param("assignedToUserId") Long assignedToUserId,
			@Param("pattern") String pattern, Pageable pageable);

	Page<Ticket> findAllByTenantIdAndRaisedByUserId(Long tenantId, Long raisedByUserId, Pageable pageable);

	Optional<Ticket> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
