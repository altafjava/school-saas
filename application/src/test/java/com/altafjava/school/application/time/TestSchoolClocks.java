package com.altafjava.school.application.time;

import java.time.Clock;
import java.time.ZoneId;
import com.altafjava.platform.application.tenant.TenantFormattingService;

/** A real {@link SchoolClock} for unit tests, with every tenant in one fixed zone. */
public final class TestSchoolClocks {

	private TestSchoolClocks() {
	}

	/** Every tenant on the server's zone, so expectations built from {@code LocalDate.now()} hold. */
	public static SchoolClock systemDefault() {
		return fixed(Clock.systemDefaultZone(), ZoneId.systemDefault());
	}

	public static SchoolClock fixed(Clock clock, ZoneId tenantZone) {
		return new SchoolClock(new FixedZoneFormatting(tenantZone), clock);
	}

	private static final class FixedZoneFormatting extends TenantFormattingService {

		private final ZoneId zone;

		FixedZoneFormatting(ZoneId zone) {
			super(null, null);
			this.zone = zone;
		}

		@Override
		public ZoneId resolveZone(Long tenantId) {
			return zone;
		}
	}
}
