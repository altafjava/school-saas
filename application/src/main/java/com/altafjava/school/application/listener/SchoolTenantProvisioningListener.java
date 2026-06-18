package com.altafjava.school.application.listener;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.altafjava.platform.application.event.events.TenantCreatedEvent;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;

/**
 * Reacts to a new tenant being provisioned by the platform.
 * Seeds school-specific default data: academic year, default fee structure names, grade categories.
 *
 * Runs asynchronously — TenantContext is propagated automatically via the platform's TaskDecorator.
 */
@Component
public class SchoolTenantProvisioningListener {

	private static final Logger log = LoggerFactory.getLogger(SchoolTenantProvisioningListener.class);

	private final AcademicYearRepository academicYearRepository;

	public SchoolTenantProvisioningListener(AcademicYearRepository academicYearRepository) {
		this.academicYearRepository = academicYearRepository;
	}

	@Async
	@Transactional
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onTenantCreated(TenantCreatedEvent event) {
		log.info("action=school-tenant-provisioning tenantId={} tenantType={}", event.tenantId(), event.tenantType());
		TenantContext.setTenant(event.tenantId(), null);
		try {
			seedDefaultAcademicYear(event.tenantId());
			log.info("action=school-tenant-provisioning-complete tenantId={}", event.tenantId());
		} finally {
			TenantContext.clear();
		}
	}

	private void seedDefaultAcademicYear(Long tenantId) {
		LocalDate now = LocalDate.now();
		int year = now.getYear();
		String name = year + "-" + (year + 1);

		if (academicYearRepository.existsByNameAndTenantId(name, tenantId)) {
			log.info("action=seed-academic-year-skipped tenantId={} name={} reason=already-exists", tenantId, name);
			return;
		}

		AcademicYear academicYear = AcademicYear.create(
				name,
				LocalDate.of(year, 4, 1),
				LocalDate.of(year + 1, 3, 31),
				true);
		academicYearRepository.save(academicYear);
		log.info("action=seed-academic-year-created tenantId={} name={}", tenantId, name);
	}
}
