package com.altafjava.school.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.infrastructure.cache.FallbackCacheErrorHandler;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.curriculum.model.GradeBand;
import com.altafjava.school.domain.curriculum.model.GradingScale;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.curriculum.repository.GradingScaleRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleThresholdRepository;

/** Redis going down must cost speed, not grade entry: every cached read falls through to the database. */
@SpringJUnitConfig(GradingScaleCacheOutageTest.Config.class)
class GradingScaleCacheOutageTest {

	@Configuration
	@EnableCaching
	static class Config implements CachingConfigurer {

		@Bean
		@Override
		public CacheManager cacheManager() {
			CacheManager manager = mock(CacheManager.class);
			when(manager.getCache(any())).thenReturn(new DownCache());
			return manager;
		}

		@Override
		public CacheErrorHandler errorHandler() {
			return new FallbackCacheErrorHandler();
		}

		@Bean("tenantAwareCacheKeyGenerator")
		public KeyGenerator tenantAwareCacheKeyGenerator() {
			return new SimpleKeyGenerator();
		}

		@Bean
		public GradingScaleThresholdRepository thresholdRepository() {
			return mock(GradingScaleThresholdRepository.class);
		}

		@Bean
		public GradingScaleRepository scaleRepository() {
			return mock(GradingScaleRepository.class);
		}

		@Bean
		public ClassroomRepository classroomRepository() {
			return mock(ClassroomRepository.class);
		}

		@Bean
		public GradingScaleService gradingScaleService(GradingScaleThresholdRepository thresholds,
				GradingScaleRepository scales, ClassroomRepository classrooms) throws Exception {
			Constructor<?> constructor = GradingScaleService.class.getConstructors()[0];
			Object[] arguments = Arrays.stream(constructor.getParameterTypes()).map(type -> {
				if (type == GradingScaleThresholdRepository.class) {
					return thresholds;
				}
				if (type == GradingScaleRepository.class) {
					return scales;
				}
				return type == ClassroomRepository.class ? classrooms : mock(type);
			}).toArray();
			return (GradingScaleService) constructor.newInstance(arguments);
		}
	}

	/** Behaves as Redis does while unreachable: every operation throws. */
	static class DownCache extends AbstractValueAdaptingCache {

		DownCache() {
			super(false);
		}

		@Override
		public String getName() {
			return "down";
		}

		@Override
		public Object getNativeCache() {
			return this;
		}

		@Override
		protected Object lookup(Object key) {
			throw new IllegalStateException("Redis is unreachable");
		}

		@Override
		public <T> T get(Object key, Callable<T> valueLoader) {
			throw new IllegalStateException("Redis is unreachable");
		}

		@Override
		public void put(Object key, Object value) {
			throw new IllegalStateException("Redis is unreachable");
		}

		@Override
		public void evict(Object key) {
			throw new IllegalStateException("Redis is unreachable");
		}

		@Override
		public void clear() {
			throw new IllegalStateException("Redis is unreachable");
		}
	}

	@Autowired
	private GradingScaleService service;
	@Autowired
	private GradingScaleThresholdRepository thresholdRepository;
	@Autowired
	private GradingScaleRepository scaleRepository;

	@BeforeEach
	void bindTenant() {
		TenantContext.ForTesting.setCurrentTenant(1L, UUID.randomUUID(), "school", TenantType.SHARED);
	}

	@AfterEach
	void clearTenant() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void cacheOutage_readsTheScaleFromTheDatabase() {
		GradingScale scale = mock(GradingScale.class);
		when(scale.getId()).thenReturn(3L);
		when(scaleRepository.findByIsDefaultTrueAndTenantId(anyLong())).thenReturn(Optional.of(scale));
		GradingScaleThreshold threshold = mock(GradingScaleThreshold.class);
		when(threshold.getMinPercentage()).thenReturn(new BigDecimal("90"));
		when(threshold.getLetter()).thenReturn("A");
		when(thresholdRepository.findAllByGradingScaleIdAndTenantId(anyLong(), anyLong()))
				.thenReturn(List.of(threshold));

		List<GradeBand> bands = service.resolveEffectiveThresholds(null);

		assertThat(bands).hasSize(1);
	}
}
