package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.vo.RateLimitStatusVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final int PER_MINUTE_LIMIT = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;
    private final AuditPort auditPort;

    @Override
    public RateLimitStatusVO checkOrThrow(Long userId, String limiterKey) {
        String redisKey = "rate-limit:" + limiterKey + ":user:" + userId;
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(redisKey, WINDOW);
        }
        Long ttl = stringRedisTemplate.getExpire(redisKey);
        int retryAfter = ttl == null || ttl < 0 ? (int) WINDOW.getSeconds() : ttl.intValue();
        int currentCount = count == null ? 0 : count.intValue();
        boolean limited = currentCount > PER_MINUTE_LIMIT;

        auditPort.logRateLimitEvent(new AuditPort.RateLimitEvent(
                MDC.get("traceId"),
                "user:" + userId,
                redisKey,
                PER_MINUTE_LIMIT,
                currentCount,
                limited ? "limited" : "allowed",
                retryAfter
        ));

        if (limited) {
            throw new BizException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后重试（retry_after=" + retryAfter + "）");
        }
        return RateLimitStatusVO.builder()
                .limit(PER_MINUTE_LIMIT)
                .currentCount(currentCount)
                .retryAfterSeconds(retryAfter)
                .limited(false)
                .build();
    }
}
