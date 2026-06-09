package com.axiqra.core.adapter;

import com.axiqra.common.port.QuotaInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

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

    @Test
    void shouldReturnDefaultQuotaInfo() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("quota:connect:user:1:"))).thenReturn("2");

        RedisQuotaAdapter adapter = new RedisQuotaAdapter(stringRedisTemplate);
        QuotaInfo info = adapter.getQuotaInfo(1L);

        assertEquals(2, info.getUsed());
        assertEquals(1998, info.getRemaining());
        assertEquals(2000, info.getLimit());
    }

    @Test
    void shouldConsumeWhenUnderLimit() {
        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), eq("2000"), any(String.class)))
                .thenReturn(1L);

        RedisQuotaAdapter adapter = new RedisQuotaAdapter(stringRedisTemplate);

        assertTrue(adapter.tryConsumeQuota(1L));
        verify(stringRedisTemplate).execute(any(RedisScript.class), any(List.class), eq("2000"), any(String.class));
    }

    @Test
    void shouldRejectWhenExceedingLimit() {
        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), eq("2000"), any(String.class)))
                .thenReturn(0L);

        RedisQuotaAdapter adapter = new RedisQuotaAdapter(stringRedisTemplate);

        assertFalse(adapter.tryConsumeQuota(1L));
        verify(stringRedisTemplate).execute(any(RedisScript.class), any(List.class), eq("2000"), any(String.class));
        verifyNoInteractions(valueOperations);
    }
}
