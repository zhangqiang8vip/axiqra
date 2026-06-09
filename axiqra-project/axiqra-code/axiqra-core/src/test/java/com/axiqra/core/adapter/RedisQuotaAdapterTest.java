package com.axiqra.core.adapter;

import com.axiqra.common.port.QuotaInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisQuotaAdapter 单元测试")
class RedisQuotaAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisQuotaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisQuotaAdapter(stringRedisTemplate);
        ReflectionTestUtils.setField(adapter, "dailyLimit", 2000);
        ReflectionTestUtils.setField(adapter, "ttlHours", 48);
    }

    @Test
    void shouldReturnDefaultQuotaInfo() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("quota:connect:user:1:"))).thenReturn("2");

        QuotaInfo info = adapter.getQuotaInfo(1L);

        assertEquals(2, info.getUsed());
        assertEquals(1998, info.getRemaining());
        assertEquals(2000, info.getLimit());
    }

    @Test
    void shouldConsumeWhenUnderLimit() {
        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), any(String.class), any(String.class)))
                .thenReturn(1L);

        assertTrue(adapter.tryConsumeQuota(1L));
        verify(stringRedisTemplate).execute(any(RedisScript.class), any(List.class), any(String.class), any(String.class));
    }

    @Test
    void shouldRejectWhenExceedingLimit() {
        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), any(String.class), any(String.class)))
                .thenReturn(0L);

        assertFalse(adapter.tryConsumeQuota(1L));
        verify(stringRedisTemplate).execute(any(RedisScript.class), any(List.class), any(String.class), any(String.class));
        verifyNoInteractions(valueOperations);
    }
}
