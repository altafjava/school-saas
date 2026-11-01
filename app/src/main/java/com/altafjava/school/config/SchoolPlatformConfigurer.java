package com.altafjava.school.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.PlatformConfigurer;
import com.altafjava.platform.core.cache.CacheRegion;
import com.altafjava.platform.core.privacy.DomainPiiHandler;
import com.altafjava.platform.core.privacy.DomainRetentionHandler;
import com.altafjava.platform.core.security.permission.PermissionDefinition;
import com.altafjava.platform.core.sync.OfflineSyncEntityHandler;
import com.altafjava.school.application.privacy.SchoolDataRetentionHandler;
import com.altafjava.school.application.privacy.StudentGuardianPiiHandler;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.application.sync.AttendanceOfflineSyncHandler;
import com.altafjava.school.domain.curriculum.model.GradeBand;
import com.altafjava.school.domain.metrics.SchoolMetricTypes;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

/**
 * School-specific platform configuration.
 * Only overrides defaults that the school domain needs to change.
 */
@Component
public class SchoolPlatformConfigurer implements PlatformConfigurer {

	private static final Duration GRADING_SCALE_CACHE_TTL = Duration.ofHours(1);

	// Built once from the constructor-injected singleton handler bean, not inside
	// offlineSyncEntityHandlers() itself — that method is called on every sync operation, and a
	// handler with any in-memory state would lose it between calls if reconstructed each time
	// (see PlatformConfigurer#offlineSyncEntityHandlers's Javadoc).
	private final Map<String, OfflineSyncEntityHandler> offlineSyncEntityHandlers;
	private final StudentGuardianPiiHandler studentGuardianPiiHandler;
	private final SchoolDataRetentionHandler schoolDataRetentionHandler;

	public SchoolPlatformConfigurer(AttendanceOfflineSyncHandler attendanceOfflineSyncHandler,
			StudentGuardianPiiHandler studentGuardianPiiHandler,
			SchoolDataRetentionHandler schoolDataRetentionHandler) {
		this.offlineSyncEntityHandlers = Map.of("attendance", attendanceOfflineSyncHandler);
		this.studentGuardianPiiHandler = studentGuardianPiiHandler;
		this.schoolDataRetentionHandler = schoolDataRetentionHandler;
	}

	@Override
	public Map<String, OfflineSyncEntityHandler> offlineSyncEntityHandlers() {
		return offlineSyncEntityHandlers;
	}

	@Override
	public Optional<DomainPiiHandler> domainPiiHandler() {
		return Optional.of(studentGuardianPiiHandler);
	}

	@Override
	public Optional<DomainRetentionHandler> domainRetentionHandler() {
		return Optional.of(schoolDataRetentionHandler);
	}

	@Override
	public String platformName() {
		return "School Management Platform";
	}

	@Override
	public String platformVersion() {
		return "1.0.0";
	}

	@Override
	public Duration accessTokenExpiry() {
		return Duration.ofHours(8);
	}

	@Override
	public Duration refreshTokenExpiry() {
		return Duration.ofDays(7);
	}

	@Override
	public Set<String> enabledNotificationChannels() {
		return Set.of("EMAIL", "IN_APP", "SMS");
	}

	@Override
	public int maxTenantsPerInstance() {
		return 50;
	}

	@Override
	public List<String> domainTenantChangelogPaths() {
		return List.of("db/domain/changelog-master.xml");
	}

	@Override
	public List<CacheRegion> domainCacheRegions() {
		return List.of(CacheRegion.listOf(GradingScaleService.CACHE_GRADING_SCALE_THRESHOLDS, GradeBand.class,
				GRADING_SCALE_CACHE_TTL));
	}

	@Override
	public Set<String> additionalMetricTypes() {
		return SchoolMetricTypes.ALL;
	}

	@Override
	public Set<PermissionDefinition> domainPermissionCatalog() {
		return SchoolPermissions.CATALOG;
	}
}
