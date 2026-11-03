package com.altafjava.school.application.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.core.tenant.TenantContext;

/**
 * The school's wall clock. School dates and times are local to the tenant's time zone, which can
 * differ from the server's by most of a day, so school code reads "now" only from here.
 */
@Component
public class SchoolClock {

	private final TenantFormattingService tenantFormattingService;
	private final Clock clock;

	@Autowired
	public SchoolClock(TenantFormattingService tenantFormattingService) {
		this(tenantFormattingService, Clock.systemUTC());
	}

	SchoolClock(TenantFormattingService tenantFormattingService, Clock clock) {
		this.tenantFormattingService = tenantFormattingService;
		this.clock = clock;
	}

	public ZoneId zone(Long tenantId) {
		return tenantFormattingService.resolveZone(tenantId);
	}

	public LocalDateTime now(Long tenantId) {
		return LocalDateTime.now(clock.withZone(zone(tenantId)));
	}

	public LocalDate today(Long tenantId) {
		return now(tenantId).toLocalDate();
	}

	public YearMonth currentMonth(Long tenantId) {
		return YearMonth.from(now(tenantId));
	}

	/** The current request's or job's tenant. */
	public LocalDateTime now() {
		return now(TenantContext.getCurrentTenantId());
	}

	/** The current request's or job's tenant. */
	public LocalDate today() {
		return today(TenantContext.getCurrentTenantId());
	}
}
