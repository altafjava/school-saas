package com.altafjava.school.domain.event.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.event.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

	// Every filter is optional (null matches all). `from` is inclusive and `before` exclusive.
	@Query("""
			SELECT e FROM Event e
			WHERE e.tenantId = :tenantId
			  AND (:#{#from == null} = TRUE OR e.eventDate >= :from)
			  AND (:#{#before == null} = TRUE OR e.eventDate < :before)
			  AND (:#{#pattern == null} = TRUE OR LOWER(e.title) LIKE :pattern ESCAPE '!' OR LOWER(e.location) LIKE :pattern ESCAPE '!')
			""")
	Page<Event> search(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
			@Param("before") LocalDateTime before, @Param("pattern") String pattern, Pageable pageable);

	Optional<Event> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Event> findByIdAndTenantId(Long id, Long tenantId);

	long countByTenantIdAndActiveTrueAndEventDateAfter(Long tenantId, LocalDateTime after);
}
