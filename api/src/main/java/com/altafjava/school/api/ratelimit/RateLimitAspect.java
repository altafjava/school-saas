package com.altafjava.school.api.ratelimit;

import java.time.Duration;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.platform.core.tenant.TenantContext;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;

/**
 * Enforces {@link RateLimited} — a stricter per-operation limit layered on platform's flat
 * per-tenant {@code RateLimitInterceptor}, reusing its Redis-backed {@code ProxyManager<String>}.
 * Rejects with {@link BusinessException} (400): platform's shared exception handler has no
 * generic 429 mapping to hook into.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

	private final ProxyManager<String> proxyManager;

	@Around("@annotation(rateLimited)")
	public Object enforce(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
		Long tenantId = TenantContext.getCurrentTenantId();
		String bucketKey = "school:ratelimit:" + rateLimited.key() + ":" + tenantId;

		Bandwidth bandwidth = Bandwidth.builder()
				.capacity(rateLimited.capacity())
				.refillGreedy(rateLimited.capacity(), Duration.ofMinutes(rateLimited.periodMinutes()))
				.build();
		BucketConfiguration configuration = BucketConfiguration.builder().addLimit(bandwidth).build();
		Bucket bucket = proxyManager.builder().build(bucketKey, () -> configuration);

		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (!probe.isConsumed()) {
			long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
			throw new BusinessException(PlatformErrorCode.RATE_LIMIT_EXCEEDED,
					"Rate limit exceeded for this operation — try again in "
							+ waitSeconds + " seconds",
					Map.of("retryAfterSeconds", waitSeconds));
		}

		return joinPoint.proceed();
	}
}
