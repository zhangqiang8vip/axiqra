package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.CandidateSeedEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * CandidateSeed Mapper
 */
@Mapper
public interface CandidateSeedMapper extends BaseMapper<CandidateSeedEntity> {

    @Select("SELECT * FROM axiqra_candidate_seed WHERE query_hash = #{queryHash} AND workspace_id = #{workspaceId} AND is_deleted = FALSE LIMIT 1")
    CandidateSeedEntity selectByQueryHashAndWorkspaceId(@Param("queryHash") String queryHash,
                                                        @Param("workspaceId") Long workspaceId);
}
