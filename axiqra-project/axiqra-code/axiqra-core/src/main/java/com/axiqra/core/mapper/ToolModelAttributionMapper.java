package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.ToolModelAttributionEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;
import java.util.List;

/**
 * ToolModelAttribution Mapper
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Mapper
public interface ToolModelAttributionMapper extends BaseMapper<ToolModelAttributionEntity> {

    @Select("SELECT * FROM axiqra_tool_model_attribution WHERE solution_id = #{solutionId} AND is_deleted = FALSE")
    List<ToolModelAttributionEntity> selectBySolutionId(@Param("solutionId") Long solutionId);

    @Select("SELECT * FROM axiqra_tool_model_attribution WHERE request_id = #{requestId} AND is_deleted = FALSE LIMIT 1")
    ToolModelAttributionEntity selectByRequestId(@Param("requestId") String requestId);

    @Select("SELECT * FROM axiqra_tool_model_attribution WHERE gmt_create >= #{since} AND is_deleted = FALSE")
    List<ToolModelAttributionEntity> selectRecentAttributions(@Param("since") Instant since);
}
