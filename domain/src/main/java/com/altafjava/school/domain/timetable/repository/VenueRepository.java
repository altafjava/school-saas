package com.altafjava.school.domain.timetable.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.timetable.model.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {

	Page<Venue> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Venue> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Venue> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
