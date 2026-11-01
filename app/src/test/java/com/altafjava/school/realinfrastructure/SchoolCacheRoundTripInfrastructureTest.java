package com.altafjava.school.realinfrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import com.altafjava.platform.testsupport.CachedRegions;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.base.SchoolInfrastructureTestBase;
import com.altafjava.school.domain.curriculum.model.GradeBand;

/** Every region school-saas declares must hold its value type through real Redis. */
class SchoolCacheRoundTripInfrastructureTest extends SchoolInfrastructureTestBase {

	@Autowired
	private CacheManager cacheManager;

	@Test
	void gradingScale_keepsItsDecimalsExactly() {
		Cache cache = cacheManager.getCache(GradingScaleService.CACHE_GRADING_SCALE_THRESHOLDS);
		List<GradeBand> bands = List.of(new GradeBand("A", new BigDecimal("90.50"), new BigDecimal("4.00")));
		String key = UUID.randomUUID().toString();

		cache.put(key, bands);

		// The Redis cache writer stores asynchronously, so a read can briefly precede the write.
		await().atMost(Duration.ofSeconds(5)).until(() -> cache.get(key) != null);
		assertThat(cache.get(key).get()).isEqualTo(bands);
	}

	@Test
	void everyCacheableRegion_isDeclaredAndHasASample() {
		Set<String> sampled = Set.of(GradingScaleService.CACHE_GRADING_SCALE_THRESHOLDS);

		Set<String> used = CachedRegions.regionsIn("com.altafjava.school", Set.of());
		assertThat(sampled).containsAll(used);
		assertThat(cacheManager.getCacheNames()).containsAll(used);
	}
}
