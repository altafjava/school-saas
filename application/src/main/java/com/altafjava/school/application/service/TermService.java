package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@Service
public class TermService {

	private final TermRepository termRepository;
	private final AcademicYearRepository academicYearRepository;

	public TermService(TermRepository termRepository, AcademicYearRepository academicYearRepository) {
		this.termRepository = termRepository;
		this.academicYearRepository = academicYearRepository;
	}

	@Transactional(readOnly = true)
	public Page<Term> listTerms(Pageable pageable) {
		return termRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Term findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return termRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Term not found: " + publicId));
	}

	@Transactional
	public Term create(String name, LocalDate startDate, LocalDate endDate, String academicYearPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long academicYearId = academicYearRepository
				.findByPublicIdAndTenantId(UUID.fromString(academicYearPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("AcademicYear not found: " + academicYearPublicId))
				.getId();
		if (termRepository.existsByNameAndAcademicYearIdAndTenantId(name, academicYearId, tenantId)) {
			throw new IllegalArgumentException("Term already exists: " + name + " for this academic year");
		}
		Term term = Term.create(name, startDate, endDate, academicYearId);
		if (!startDate.isAfter(LocalDate.now()) && !endDate.isBefore(LocalDate.now())) {
			termRepository.findCurrentByTenantId(tenantId).ifPresent(Term::markNotCurrent);
			term.markCurrent();
		}
		return termRepository.save(term);
	}
}
