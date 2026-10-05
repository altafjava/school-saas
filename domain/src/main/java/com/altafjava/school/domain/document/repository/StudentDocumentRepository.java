package com.altafjava.school.domain.document.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.document.model.StudentDocument;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

	@Query("SELECT d FROM StudentDocument d WHERE d.tenantId = :tenantId AND d.publicId = :publicId")
	Optional<StudentDocument> findByPublicIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("publicId") UUID publicId);

	@Query("SELECT d FROM StudentDocument d WHERE d.tenantId = :tenantId AND d.studentId = :studentId")
	Page<StudentDocument> findByStudentIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId, Pageable pageable);

	@Query("SELECT d FROM StudentDocument d WHERE d.tenantId = :tenantId AND d.admissionId = :admissionId")
	Page<StudentDocument> findByAdmissionIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("admissionId") Long admissionId, Pageable pageable);
}
