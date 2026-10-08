package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.admission.ApplicationFeePolicy;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdmissionSettingsService {

	private final ApplicationFeePolicy applicationFeePolicy;

	@Transactional(readOnly = true)
	public Optional<BigDecimal> getApplicationFee() {
		return applicationFeePolicy.feeFor(TenantContext.getCurrentTenantId());
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "AdmissionSettings", details = "Application fee changed")
	public void setApplicationFee(BigDecimal fee) {
		applicationFeePolicy.set(TenantContext.getCurrentTenantId(), fee);
	}
}
