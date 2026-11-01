package com.altafjava.school.realinfrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import com.altafjava.platform.testsupport.CachedValueTypes;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.base.SchoolInfrastructureTestBase;
import com.altafjava.school.domain.curriculum.model.Board;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;

/** Every value type school-saas caches must survive a write to and a read from real Redis. */
class SchoolCacheRoundTripInfrastructureTest extends SchoolInfrastructureTestBase {

	@Autowired
	private CacheManager cacheManager;

	static Stream<Object> cachedValues() {
		return Stream.of(
				Board.create("CBSE", "CBSE", "Central board"),
				Curriculum.create(1L, "Primary", "PRI", "Primary curriculum"),
				GradingScaleThreshold.create(1L, "A", new BigDecimal("90.00"), new BigDecimal("4.00")));
	}

	@ParameterizedTest
	@MethodSource("cachedValues")
	void cachedValue_survivesRedisRoundTrip(Object value) {
		assertThat(roundTrip(value)).usingRecursiveComparison()
				.withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.isEqualTo(value);
	}

	@Test
	void cachedList_survivesRedisRoundTrip() {
		List<GradingScaleThreshold> thresholds = new ArrayList<>(List.of(
				GradingScaleThreshold.create(1L, "A", new BigDecimal("90.00"), new BigDecimal("4.00"))));

		assertThat(roundTrip(thresholds)).asList().hasSize(1);
	}

	@Test
	void everyCachedType_hasARoundTripSample() {
		Set<Class<?>> sampled = new HashSet<>();
		cachedValues().forEach(value -> sampled.add(value.getClass()));

		assertThat(sampled).containsAll(CachedValueTypes.in("com.altafjava.school", Set.of()));
	}

	private Object roundTrip(Object value) {
		Cache cache = cacheManager.getCache(GradingScaleService.CACHE_GRADING_SCALE_THRESHOLDS);
		String key = UUID.randomUUID().toString();
		cache.put(key, value);
		// The Redis cache writer stores asynchronously, so a read can briefly precede the write.
		await().atMost(Duration.ofSeconds(5)).until(() -> cache.get(key) != null);
		return cache.get(key).get();
	}
}
