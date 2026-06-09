package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.ConnectSessionEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConnectSessionMapper extends BaseMapper<ConnectSessionEntity> {

    @Select("SELECT * FROM axiqra_connect_session WHERE session_id = #{sessionId} AND tenant_id = #{tenantId} AND is_deleted = FALSE LIMIT 1")
    ConnectSessionEntity selectBySessionId(@Param("sessionId") String sessionId, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM axiqra_connect_session WHERE user_id = #{userId} AND tenant_id = #{tenantId} AND is_deleted = FALSE ORDER BY gmt_create DESC")
    List<ConnectSessionEntity> selectByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}
