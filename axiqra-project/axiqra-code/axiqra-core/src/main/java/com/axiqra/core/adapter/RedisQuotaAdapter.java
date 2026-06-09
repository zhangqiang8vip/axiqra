package com.axiqra.core.adapter;

import com.axiqra.common.port.QuotaInfo;
import com.axiqra.common.port.QuotaPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisQuotaAdapter implements QuotaPort {

    private static final Duration DEFAULT_TTL = Duration.ofDays(2);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String CONSUME_QUOTA_SCRIPT = "local current = redis.call('GET', KEYS[1]) "
            + "if current and tonumber(current) >= tonumber(ARGV[1]) then return 0 end "
            + "local newVal = redis.call('INCR', KEYS[1]) "
            + "if newVal == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2])) end "
            + "return newVal";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${axiqra.quota.daily-limit:2000}")
    private int dailyLimit;

    @Value("${axiqra.quota.ttl-hours:48}")
    private int ttlHours;

    @Override
    public QuotaInfo getQuotaInfo(Long userId) {
        int used = getUsedQuota(userId);
        return QuotaInfo.of(used, dailyLimit);
    }

    @Override
    public boolean tryConsumeQuota(Long userId) {
        int attempt = 0;
        DataAccessException lastException = null;
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                Long result = stringRedisTemplate.execute(
                        RedisScript.of(CONSUME_QUOTA_SCRIPT, Long.class),
                        Collections.singletonList(quotaKey(userId)),
                        String.valueOf(dailyLimit),
                        String.valueOf(ttlUntilNextReset().getSeconds())
                );
                return result != null && result > 0 && result <= dailyLimit;
            } catch (DataAccessException e) {
                lastException = e;
                attempt++;
                log.warn("consume quota failed for userId={}, attempt={}/{}: {}",
                        userId, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep((long) Math.pow(2, attempt) * 100L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        log.error("consume quota exhausted retries for userId={}, last error: {}", userId, lastException != null ? lastException.getMessage() : "unknown");
        return false;
    }

    @Override
    public void resetDailyQuota(Long userId) {
        stringRedisTemplate.delete(quotaKey(userId));
    }

    @Override
    public int getUsedQuota(Long userId) {
        String value = stringRedisTemplate.opsForValue().get(quotaKey(userId));
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("invalid quota value in redis, userId={}, value={}", userId, value);
            return 0;
        }
    }

    private String quotaKey(Long userId) {
        return "quota:connect:user:" + userId + ":" + OffsetDateTime.now(ZoneOffset.UTC).toLocalDate();
    }

    private Duration ttlUntilNextReset() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime nextReset = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
        Duration ttl = Duration.between(now, nextReset);
        if (ttl.isNegative() || ttl.isZero()) {
            return Duration.ofHours(ttlHours > 0 ? ttlHours : 48);
        }
        return ttl;
    }
}
