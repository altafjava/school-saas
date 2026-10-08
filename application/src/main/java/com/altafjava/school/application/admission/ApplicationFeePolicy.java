package com.altafjava.school.application.admission;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;

/**
 * The school's application fee — a tenant-owned operational setting (stored like
 * {@code GuardianRegistrationSettingsService}'s mode), not entitlement gating. Absent, unparseable
 * or non-positive all mean "no fee": a corrupt value must never make applications unsubmittable.
 */
@Component
@RequiredArgsConstructor
public class ApplicationFeePolicy {

	static final String SETTING_KEY = "school.admission.application-fee";

	private final TenantSettingOverrideService tenantSettingOverrideService;

	@Transactional(readOnly = true)
	public Optional<BigDecimal> feeFor(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, SETTING_KEY)
				.flatMap(ApplicationFeePolicy::parsePositive);
	}

	@Transactional
	public void set(Long tenantId, BigDecimal fee) {
		if (fee == null || fee.signum() == 0) {
			tenantSettingOverrideService.remove(tenantId, SETTING_KEY);
			return;
		}
		if (fee.signum() < 0 || fee.scale() > 2) {
			throw new BusinessException("The application fee must be a positive amount with at most 2 decimals");
		}
		tenantSettingOverrideService.set(tenantId, SETTING_KEY, fee.toPlainString());
	}

	private static Optional<BigDecimal> parsePositive(String value) {
		try {
			BigDecimal fee = new BigDecimal(value);
			return fee.signum() > 0 ? Optional.of(fee) : Optional.empty();
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
