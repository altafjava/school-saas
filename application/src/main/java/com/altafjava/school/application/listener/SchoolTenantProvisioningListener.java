package com.altafjava.school.application.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import com.altafjava.platform.application.event.events.TenantCreatedEvent;

/**
 * Reacts to a new tenant being provisioned by the platform.
 * Seeds school-specific default data (academic year, grade categories, default classroom structure).
 *
 * Runs asynchronously so it does not extend the tenant registration transaction.
 * TenantContext is propagated automatically via the platform's TaskDecorator.
 */
@Component
public class SchoolTenantProvisioningListener {

    private static final Logger log = LoggerFactory.getLogger(SchoolTenantProvisioningListener.class);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        log.info("action=school-tenant-provisioning tenantId={} tenantType={}", event.tenantId(), event.tenantType());
        seedDefaultAcademicYear(event.tenantId());
        log.info("action=school-tenant-provisioning-complete tenantId={}", event.tenantId());
    }

    private void seedDefaultAcademicYear(Long tenantId) {
        // Phase 5 scope: log only — full seeding added when AcademicYear entity is introduced
        log.info("action=seed-academic-year tenantId={}", tenantId);
    }
}
