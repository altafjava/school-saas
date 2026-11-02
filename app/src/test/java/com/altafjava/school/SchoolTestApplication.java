package com.altafjava.school;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import com.altafjava.platform.domain.feature.repository.FeatureFlagRepository;
import com.altafjava.school.application.security.SchoolFeatures;

// SecurityConfig is imported automatically via PlatformAutoConfiguration — no @Import needed here
@SpringBootApplication(scanBasePackages = { "com.altafjava.school", "com.altafjava.platform" })
public class SchoolTestApplication {

	// E2E suites exercise every module, so features start on; FeatureEntitlementE2ETest proves the refusals.
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	ApplicationRunner enableEveryFeatureForTests(FeatureFlagRepository flags) {
		return args -> SchoolFeatures.CATALOG.forEach(feature -> flags.findByKey(feature.key()).ifPresent(flag -> {
			flag.setEnabled(true);
			flags.save(flag);
		}));
	}

	public static void main(String[] args) {
		SpringApplication.run(SchoolTestApplication.class, args);
	}
}
