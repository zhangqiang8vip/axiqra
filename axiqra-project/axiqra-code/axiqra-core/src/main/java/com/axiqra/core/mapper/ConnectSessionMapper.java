package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.ConnectSessionEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Insert;
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

    @Insert("""
            INSERT INTO axiqra_connect_session (
                session_id, user_id, channel, tool_type, target_type, target_id,
                workspace_id, status, risk_level, confirmation_obtained, expires_at,
                doctor_status, doctor_snapshot, history_snapshot, tenant_id,
                is_deleted, gmt_create, gmt_modified, version
            ) VALUES (
                #{sessionId}, #{userId}, #{channel}, #{toolType}, #{targetType}, #{targetId},
                #{workspaceId}, #{status}, #{riskLevel}, #{confirmationObtained}, #{expiresAt},
                #{doctorStatus}, #{doctorSnapshot}, #{historySnapshot}, #{tenantId},
                #{isDeleted}, #{gmtCreate}, #{gmtModified}, #{version}
            )
            ON CONFLICT (session_id) DO UPDATE SET
                user_id = EXCLUDED.user_id,
                channel = EXCLUDED.channel,
                tool_type = EXCLUDED.tool_type,
                target_type = EXCLUDED.target_type,
                target_id = EXCLUDED.target_id,
                workspace_id = EXCLUDED.workspace_id,
                status = EXCLUDED.status,
                risk_level = EXCLUDED.risk_level,
                confirmation_obtained = EXCLUDED.confirmation_obtained,
                expires_at = EXCLUDED.expires_at,
                doctor_status = EXCLUDED.doctor_status,
                doctor_snapshot = EXCLUDED.doctor_snapshot,
                history_snapshot = EXCLUDED.history_snapshot,
                tenant_id = EXCLUDED.tenant_id,
                is_deleted = EXCLUDED.is_deleted,
                gmt_modified = EXCLUDED.gmt_modified,
                version = axiqra_connect_session.version + 1
            WHERE axiqra_connect_session.tenant_id = EXCLUDED.tenant_id
            """)
    int upsert(ConnectSessionEntity entity);
}
