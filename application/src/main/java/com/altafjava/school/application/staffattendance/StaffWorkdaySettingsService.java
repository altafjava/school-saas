package com.altafjava.school.application.staffattendance;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.staffattendance.model.StaffWorkday;
import lombok.RequiredArgsConstructor;

/**
 * Each school sets its own staff workday, stored as tenant setting overrides. A value that cannot be
 * read falls back to the default rather than blocking every check-in.
 */
@Service
@RequiredArgsConstructor
public class StaffWorkdaySettingsService {

	static final String START_KEY = "school.staff-attendance.workday-start";
	static final String END_KEY = "school.staff-attendance.workday-end";
	static final String GRACE_KEY = "school.staff-attendance.late-grace-minutes";
	static final String HALF_DAY_KEY = "school.staff-attendance.half-day-minutes";

	private final TenantSettingOverrideService tenantSettingOverrideService;

	@Transactional(readOnly = true)
	public StaffWorkday current() {
		Long tenantId = TenantContext.getCurrentTenantId();
		StaffWorkday fallback = StaffWorkday.DEFAULT;
		LocalTime start = time(tenantId, START_KEY).orElse(fallback.start());
		LocalTime end = time(tenantId, END_KEY).orElse(fallback.end());
		if (!start.isBefore(end)) {
			return fallback;
		}
		return new StaffWorkday(start, end, minutes(tenantId, GRACE_KEY, 0).orElse(fallback.lateGraceMinutes()),
				minutes(tenantId, HALF_DAY_KEY, 1).orElse(fallback.halfDayMinutes()));
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "StaffWorkday", details = "Staff workday changed")
	public StaffWorkday update(StaffWorkday workday) {
		Long tenantId = TenantContext.getCurrentTenantId();
		tenantSettingOverrideService.set(tenantId, START_KEY, workday.start().toString());
		tenantSettingOverrideService.set(tenantId, END_KEY, workday.end().toString());
		tenantSettingOverrideService.set(tenantId, GRACE_KEY, String.valueOf(workday.lateGraceMinutes()));
		tenantSettingOverrideService.set(tenantId, HALF_DAY_KEY, String.valueOf(workday.halfDayMinutes()));
		return workday;
	}

	private Optional<LocalTime> time(Long tenantId, String key) {
		try {
			return tenantSettingOverrideService.get(tenantId, key).map(LocalTime::parse);
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	private Optional<Integer> minutes(Long tenantId, String key, int minimum) {
		try {
			return tenantSettingOverrideService.get(tenantId, key).map(Integer::valueOf)
					.filter(value -> value >= minimum);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
