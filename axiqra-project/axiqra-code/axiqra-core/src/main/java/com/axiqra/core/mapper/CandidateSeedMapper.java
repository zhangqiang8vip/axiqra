package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.CandidateSeedEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CandidateSeed Mapper
 */
@Mapper
public interface CandidateSeedMapper extends BaseMapper<CandidateSeedEntity> {

    @Select("SELECT * FROM axiqra_candidate_seed WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    CandidateSeedEntity selectById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_candidate_seed WHERE query_hash = #{queryHash} AND workspace_id = #{workspaceId} AND is_deleted = FALSE LIMIT 1")
    CandidateSeedEntity selectByQueryHashAndWorkspaceId(@Param("queryHash") String queryHash,
                                                        @Param("workspaceId") Long workspaceId);

    @Select("SELECT * FROM axiqra_candidate_seed WHERE author_id = #{authorId} AND is_deleted = FALSE ORDER BY gmt_create DESC")
    List<CandidateSeedEntity> selectByAuthorId(@Param("authorId") Long authorId);

    @Select("SELECT * FROM axiqra_candidate_seed WHERE workspace_id = #{workspaceId} AND is_deleted = FALSE ORDER BY gmt_create DESC")
    List<CandidateSeedEntity> selectByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
