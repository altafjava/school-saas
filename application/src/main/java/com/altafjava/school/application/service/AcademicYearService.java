package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;

@Service
public class AcademicYearService {

	private final AcademicYearRepository academicYearRepository;

	public AcademicYearService(AcademicYearRepository academicYearRepository) {
		this.academicYearRepository = academicYearRepository;
	}

	@Transactional(readOnly = true)
	public Page<AcademicYear> listAcademicYears(Pageable pageable) {
		return academicYearRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public AcademicYear findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return academicYearRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("AcademicYear not found: " + publicId));
	}

	@Transactional
	public AcademicYear create(String name, LocalDate startDate, LocalDate endDate, boolean current) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (academicYearRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new IllegalArgumentException("Academic year already exists: " + name);
		}
		AcademicYear academicYear = AcademicYear.create(name, startDate, endDate, current);
		return academicYearRepository.save(academicYear);
	}
}
