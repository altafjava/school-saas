package com.altafjava.school.domain.enquiry.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.enquiry.model.Enquiry;
import com.altafjava.school.domain.enquiry.model.EnquirySource;
import com.altafjava.school.domain.enquiry.model.EnquiryStatus;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

	Optional<Enquiry> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	/**
	 * Every filter is optional (null matches all). {@code followUpDueBy} keeps open enquiries whose next follow-up
	 * falls on or before it; the pattern comes from LikePattern.contains and matches either name.
	 */
	@Query("""
			SELECT e FROM Enquiry e
			WHERE e.tenantId = :tenantId
			  AND (:#{#status == null} = TRUE OR e.status = :status)
			  AND (:#{#source == null} = TRUE OR e.source = :source)
			  AND (:#{#assignedEmployeeId == null} = TRUE OR e.assignedEmployeeId = :assignedEmployeeId)
			  AND (:#{#followUpDueBy == null} = TRUE
			       OR (e.status IN :openStatuses AND e.nextFollowUpOn <= :followUpDueBy))
			  AND (:#{#pattern == null} = TRUE
			       OR LOWER(e.childFirstName) LIKE :pattern ESCAPE '!' OR LOWER(e.childLastName) LIKE :pattern ESCAPE '!'
			       OR LOWER(e.guardianFirstName) LIKE :pattern ESCAPE '!'
			       OR LOWER(e.guardianLastName) LIKE :pattern ESCAPE '!')
			""")
	Page<Enquiry> search(@Param("tenantId") Long tenantId, @Param("status") EnquiryStatus status,
			@Param("source") EnquirySource source, @Param("assignedEmployeeId") Long assignedEmployeeId,
			@Param("followUpDueBy") LocalDate followUpDueBy,
			@Param("openStatuses") Collection<EnquiryStatus> openStatuses, @Param("pattern") String pattern,
			Pageable pageable);

	// Finished enquiries untouched since the cutoff, for the retention job.
	@Query("SELECT e FROM Enquiry e WHERE e.tenantId = :tenantId AND e.status IN :statuses AND e.updatedAt < :cutoff")
	List<Enquiry> findFinishedBefore(@Param("tenantId") Long tenantId,
			@Param("statuses") Collection<EnquiryStatus> statuses, @Param("cutoff") Instant cutoff);
}
