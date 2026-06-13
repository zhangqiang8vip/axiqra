package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.ToolModelLeaderboardSnapshotEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * ToolModelLeaderboard Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Mapper
public interface ToolModelLeaderboardMapper extends BaseMapper<ToolModelLeaderboardSnapshotEntity> {

    @Select("<script>" +
            "SELECT * FROM axiqra_tool_model_leaderboard_snapshot " +
            "WHERE window_end = #{windowEnd} AND scope_type = #{scopeType} " +
            "<if test='scopeId != null'> AND scope_id = #{scopeId} </if>" +
            "AND is_deleted = FALSE ORDER BY rank ASC LIMIT #{limit}" +
            "</script>")
    List<ToolModelLeaderboardSnapshotEntity> selectLeaderboard(
            @Param("windowEnd") LocalDate windowEnd,
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId,
            @Param("limit") int limit);

    @Select("<script>" +
            "SELECT * FROM axiqra_tool_model_leaderboard_snapshot " +
            "WHERE window_end = #{windowEnd} AND scope_type = #{scopeType} " +
            "AND tool_name = #{toolName} " +
            "<if test='scopeId != null'> AND scope_id = #{scopeId} </if>" +
            "AND is_deleted = FALSE ORDER BY rank ASC LIMIT #{limit}" +
            "</script>")
    List<ToolModelLeaderboardSnapshotEntity> selectByToolName(
            @Param("windowEnd") LocalDate windowEnd,
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId,
            @Param("toolName") String toolName,
            @Param("limit") int limit);
}
