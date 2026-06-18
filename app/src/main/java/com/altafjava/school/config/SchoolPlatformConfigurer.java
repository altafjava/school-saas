package com.altafjava.school.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.PlatformConfigurer;

/**
 * School-specific platform configuration.
 * Only overrides defaults that the school domain needs to change.
 */
@Component
public class SchoolPlatformConfigurer implements PlatformConfigurer {

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
        // Run school domain migrations against every new SCHEMA-mode tenant schema
        return List.of("db/domain/changelog-master.xml");
    }
}
