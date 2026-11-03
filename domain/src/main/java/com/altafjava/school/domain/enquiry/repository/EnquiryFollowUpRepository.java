package com.altafjava.school.domain.enquiry.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.enquiry.model.EnquiryFollowUp;

public interface EnquiryFollowUpRepository extends JpaRepository<EnquiryFollowUp, Long> {

	@Query("SELECT f FROM EnquiryFollowUp f WHERE f.tenantId = :tenantId AND f.enquiryId = :enquiryId")
	Page<EnquiryFollowUp> findByEnquiry(@Param("tenantId") Long tenantId, @Param("enquiryId") Long enquiryId,
			Pageable pageable);

	@Query("SELECT f FROM EnquiryFollowUp f WHERE f.tenantId = :tenantId AND f.enquiryId = :enquiryId")
	List<EnquiryFollowUp> findAllByEnquiry(@Param("tenantId") Long tenantId, @Param("enquiryId") Long enquiryId);
}
