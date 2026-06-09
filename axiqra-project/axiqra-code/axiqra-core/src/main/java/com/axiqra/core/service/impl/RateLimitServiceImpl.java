package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.vo.RateLimitStatusVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;
    private final AuditPort auditPort;

    @Value("${axiqra.rate-limit.per-minute:100}")
    private int perMinuteLimit;

    @Value("${axiqra.rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Override
    public RateLimitStatusVO checkOrThrow(Long userId, String limiterKey) {
        Duration window = Duration.ofSeconds(windowSeconds);
        String redisKey = "rate-limit:" + limiterKey + ":user:" + userId;
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(redisKey, window);
        }
        Long ttl = stringRedisTemplate.getExpire(redisKey);
        int retryAfter = ttl == null || ttl < 0 ? windowSeconds : ttl.intValue();
        int currentCount = count == null ? 0 : count.intValue();
        boolean limited = currentCount > perMinuteLimit;

        auditPort.logRateLimitEvent(new AuditPort.RateLimitEvent(
                MDC.get("traceId"),
                "user:" + userId,
                redisKey,
                perMinuteLimit,
                currentCount,
                limited ? "limited" : "allowed",
                retryAfter
        ));

        if (limited) {
            throw new BizException(
                    ErrorCode.RATE_LIMITED,
                    "请求过于频繁，请稍后重试（retry_after=" + retryAfter + "）",
                    retryAfter
            );
        }
        return RateLimitStatusVO.builder()
                .limit(perMinuteLimit)
                .currentCount(currentCount)
                .retryAfterSeconds(retryAfter)
                .limited(false)
                .build();
    }
}
