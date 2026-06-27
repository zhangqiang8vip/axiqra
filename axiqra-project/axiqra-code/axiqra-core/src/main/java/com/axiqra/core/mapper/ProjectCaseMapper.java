package com.axiqra.core.mapper;

import com.axiqra.common.domain.entity.ProjectCaseEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Project Case Mapper
 */
@Mapper
public interface ProjectCaseMapper extends BaseMapper<ProjectCaseEntity> {

    @Select("SELECT * FROM axiqra_project_case WHERE id = #{id} AND is_deleted = FALSE LIMIT 1")
    ProjectCaseEntity selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM axiqra_project_case WHERE trace_id = #{traceId} AND is_deleted = FALSE LIMIT 1")
    ProjectCaseEntity selectByTraceId(@Param("traceId") Long traceId);

    @Select("<script>" +
            "SELECT * FROM axiqra_project_case WHERE is_deleted = FALSE " +
            "<if test='workspaceId != null'> AND workspace_id = #{workspaceId} </if>" +
            "<if test='authorId != null'> AND author_id = #{authorId} </if>" +
            " ORDER BY gmt_modified DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<ProjectCaseEntity> selectPageByWorkspace(@Param("workspaceId") Long workspaceId,
                                                 @Param("authorId") Long authorId,
                                                 @Param("limit") int limit,
                                                 @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM axiqra_project_case WHERE is_deleted = FALSE " +
            "<if test='workspaceId != null'> AND workspace_id = #{workspaceId} </if>" +
            "<if test='authorId != null'> AND author_id = #{authorId} </if>" +
            "</script>")
    long countByWorkspace(@Param("workspaceId") Long workspaceId, @Param("authorId") Long authorId);
}
