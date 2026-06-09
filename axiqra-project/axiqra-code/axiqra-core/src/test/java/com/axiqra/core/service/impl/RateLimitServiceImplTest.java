package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitServiceImpl 单元测试")
class RateLimitServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private AuditPort auditPort;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitServiceImpl rateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitService, "perMinuteLimit", 100);
        ReflectionTestUtils.setField(rateLimitService, "windowSeconds", 60);
    }

    @Test
    void shouldAllowWhenWithinLimit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:connect:create:user:1")).thenReturn(1L);
        when(stringRedisTemplate.getExpire("rate-limit:connect:create:user:1")).thenReturn(59L);

        var result = rateLimitService.checkOrThrow(1L, "connect:create");

        assertEquals(1, result.getCurrentCount());
        verify(auditPort).logRateLimitEvent(any());
    }

    @Test
    void shouldThrowWhenOverLimit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:connect:create:user:1")).thenReturn(101L);
        when(stringRedisTemplate.getExpire("rate-limit:connect:create:user:1")).thenReturn(40L);

        assertThrows(BizException.class, () -> rateLimitService.checkOrThrow(1L, "connect:create"));
    }
}
