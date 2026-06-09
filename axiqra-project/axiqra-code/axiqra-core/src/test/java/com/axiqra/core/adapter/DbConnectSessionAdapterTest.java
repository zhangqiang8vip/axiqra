package com.axiqra.core.adapter;

import com.axiqra.common.domain.entity.ConnectSessionEntity;
import com.axiqra.common.domain.vo.ConnectSessionVO;
import com.axiqra.core.mapper.ConnectSessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DbConnectSessionAdapter 单元测试")
class DbConnectSessionAdapterTest {

    @Mock
    private ConnectSessionMapper connectSessionMapper;

    private DbConnectSessionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DbConnectSessionAdapter(connectSessionMapper, new ObjectMapper());
    }

    @Test
    @DisplayName("listByUser 在 mapper 返回 null 时应返回空列表")
    void listByUserShouldReturnEmptyListWhenMapperReturnsNull() {
        ReflectionTestUtils.setField(adapter, "tenantId", 0L);
        when(connectSessionMapper.selectByUserId(1L, 0L)).thenReturn(null);

        List<ConnectSessionVO> result = adapter.listByUser(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listByUser 应映射实体列表")
    void listByUserShouldMapEntities() {
        ReflectionTestUtils.setField(adapter, "tenantId", 0L);
        ConnectSessionEntity entity = new ConnectSessionEntity()
                .setSessionId("s-1")
                .setUserId(1L)
                .setChannel("cli")
                .setToolType("mcp")
                .setTargetType("solution")
                .setTargetId(10L)
                .setStatus("READY");
        when(connectSessionMapper.selectByUserId(1L, 0L)).thenReturn(List.of(entity));

        List<ConnectSessionVO> result = adapter.listByUser(1L);

        assertEquals(1, result.size());
        assertEquals("s-1", result.get(0).getSessionId());
    }
}
