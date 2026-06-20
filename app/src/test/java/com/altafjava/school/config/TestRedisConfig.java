package com.altafjava.school.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import com.altafjava.platform.application.security.TokenBlacklist;
import com.altafjava.platform.domain.subscription.model.MetricType;
import com.altafjava.platform.domain.subscription.service.UsageTrackingService;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;

@TestConfiguration
@Profile("test")
public class TestRedisConfig {

	@Bean
	@Primary
	@SuppressWarnings("unchecked")
	public ProxyManager<String> proxyManager() {
		ProxyManager<String> proxyManager = Mockito.mock(ProxyManager.class);
		RemoteBucketBuilder<String> bucketBuilder = Mockito.mock(RemoteBucketBuilder.class);
		BucketProxy bucket = Mockito.mock(BucketProxy.class);
		ConsumptionProbe probe = Mockito.mock(ConsumptionProbe.class);
		Mockito.when(proxyManager.builder()).thenReturn(bucketBuilder);
		Mockito.when(bucketBuilder.build(Mockito.anyString(),
				Mockito.<java.util.function.Supplier<BucketConfiguration>>any())).thenReturn(bucket);
		Mockito.when(bucket.tryConsumeAndReturnRemaining(Mockito.anyLong())).thenReturn(probe);
		Mockito.when(probe.isConsumed()).thenReturn(true);
		Mockito.when(probe.getRemainingTokens()).thenReturn(99L);
		return proxyManager;
	}

	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory() {
		RedisConnectionFactory factory = Mockito.mock(RedisConnectionFactory.class);
		RedisConnection connection = Mockito.mock(RedisConnection.class);
		Mockito.when(factory.getConnection()).thenReturn(connection);
		Mockito.when(connection.stringCommands()).thenReturn(Mockito.mock(RedisStringCommands.class));
		Mockito.when(connection.keyCommands()).thenReturn(Mockito.mock(RedisKeyCommands.class));
		Mockito.when(connection.serverCommands()).thenReturn(Mockito.mock(RedisServerCommands.class));
		return factory;
	}

	@Bean
	@Primary
	public RedissonClient redissonClient() {
		RedissonClient client = Mockito.mock(RedissonClient.class);
		RLock lock = Mockito.mock(RLock.class);
		Mockito.when(client.getLock(Mockito.anyString())).thenReturn(lock);
		try {
			Mockito.when(lock.tryLock(Mockito.anyLong(), Mockito.anyLong(), Mockito.any())).thenReturn(true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		RRateLimiter rateLimiter = Mockito.mock(RRateLimiter.class);
		Mockito.when(client.getRateLimiter(Mockito.anyString())).thenReturn(rateLimiter);
		Mockito.when(rateLimiter.tryAcquire(Mockito.anyLong())).thenReturn(true);
		return client;
	}

	@Bean
	@Primary
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

	@Bean
	@Primary
	public UsageTrackingService usageTrackingService() {
		return new InMemoryUsageTrackingService();
	}

	@Bean
	@Primary
	public TokenBlacklist tokenBlacklist() {
		return new InMemoryTokenBlacklist();
	}

	static class InMemoryUsageTrackingService implements UsageTrackingService {
		private final Map<String, Long> usage = new ConcurrentHashMap<>();

		@Override
		public long incrementUsage(Long tenantId, MetricType metricType, long amount) {
			return usage.merge(tenantId + ":" + metricType.name(), amount, Long::sum);
		}

		@Override
		public long getCurrentUsage(Long tenantId, MetricType metricType) {
			return usage.getOrDefault(tenantId + ":" + metricType.name(), 0L);
		}

		@Override
		public void resetUsage(Long tenantId, MetricType metricType) {
			usage.remove(tenantId + ":" + metricType.name());
		}
	}

	static class InMemoryTokenBlacklist implements TokenBlacklist {
		private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

		@Override
		public void blacklist(String token, long ttlMillis) {
			blacklist.add(token);
		}

		@Override
		public boolean isBlacklisted(String token) {
			return blacklist.contains(token);
		}
	}
}
